package com.example.transbnk_uat_runner.controller;

import com.example.transbnk_uat_runner.model.ApiResult;
import com.example.transbnk_uat_runner.service.ApiRunnerService;
import com.example.transbnk_uat_runner.wrapper.service.WrapperAuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RunController {

    private static final Logger log =
            LoggerFactory.getLogger(RunController.class);

    private final ApiRunnerService service;
    private final WrapperAuditService auditService;
    private final ObjectMapper objectMapper;

    public RunController(ApiRunnerService service, WrapperAuditService auditService, ObjectMapper objectMapper) {
        this.service = service;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/{apiName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> run(@PathVariable String apiName, @RequestBody(required = false) JsonNode body) {
        log.info(" Incoming request | apiName={}", apiName);

        String requestId = extractRequestId(body);
        String requestPayload = body == null ? null : body.toString();

        try {
            ApiResult result = service.runApi(apiName, body);
            int statusCode = result.getStatusCode();

            String auditStatus = (statusCode >= 200 && statusCode < 300) ? "SUCCESS" : "FAILED";
            JsonNode fullResponseJson = result.getResponse();
            if (fullResponseJson == null) {
                fullResponseJson = objectMapper.createObjectNode();
            }

            JsonNode customizedResponseJson = service.buildCustomizedResponse(apiName, result);
            if (customizedResponseJson == null) {
                customizedResponseJson = objectMapper.createObjectNode();
            }

            auditService.writeAudit(requestId, requestPayload, fullResponseJson.toString(), auditStatus);

            log.info(
                    "Completed | apiName={} | httpStatus={} | businessStatus={}",
                    apiName,
                    result.getStatusCode(),
                    result.getBusinessStatus()
            );

            return ResponseEntity.status(statusCode).body(customizedResponseJson);
        } catch (Exception ex) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", "Failed to run api");
            error.put("error", ex.getMessage() == null ? "Internal server error" : ex.getMessage());

            auditService.writeAudit(requestId, requestPayload, error.toString(), "ERROR");

            HttpStatus status = isUnknownApiName(ex) ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(error);
        }
    }

    private static String extractRequestId(JsonNode body) {
        if (body == null) {
            return UUID.randomUUID().toString();
        }

        JsonNode requestId = body.get("requestId");
        if (requestId != null && requestId.isValueNode() && !requestId.asText().isBlank()) {
            return requestId.asText();
        }

        JsonNode requestIdSnake = body.get("request_id");
        if (requestIdSnake != null && requestIdSnake.isValueNode() && !requestIdSnake.asText().isBlank()) {
            return requestIdSnake.asText();
        }

        return UUID.randomUUID().toString();
    }

    private static boolean isUnknownApiName(Exception ex) {
        if (ex == null || ex.getMessage() == null) {
            return false;
        }
        return ex.getMessage().startsWith("Unknown apiName:");
    }
}
