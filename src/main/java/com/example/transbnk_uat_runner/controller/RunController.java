package com.example.transbnk_uat_runner.controller;

import com.example.transbnk_uat_runner.model.ApiResult;
import com.example.transbnk_uat_runner.service.ApiRunnerService;
import com.example.transbnk_uat_runner.wrapper.service.WrapperAuditService;
import com.example.transbnk_uat_runner.wrapper.service.WrapperPayloadCryptoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final WrapperPayloadCryptoService payloadCryptoService;
    private final ObjectMapper objectMapper;
    private final boolean payloadRequired;

    public RunController(
            ApiRunnerService service,
            WrapperAuditService auditService,
            WrapperPayloadCryptoService payloadCryptoService,
            ObjectMapper objectMapper,
            @Value("${wrapper.payload-required:false}") boolean payloadRequired
    ) {
        this.service = service;
        this.auditService = auditService;
        this.payloadCryptoService = payloadCryptoService;
        this.objectMapper = objectMapper;
        this.payloadRequired = payloadRequired;
    }

    @PostMapping(value = "/{apiName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> run(@PathVariable String apiName, @RequestBody(required = false) JsonNode body) {
        log.info(" Incoming request | apiName={}", apiName);

        boolean encryptedMode = payloadRequired || hasEncData(body);
        JsonNode decryptedBody = body;
        String requestPayload = body == null ? null : body.toString();

        if (hasEncData(body)) {
            try {
                String encData = extractEncData(body);
                String decryptedPayload = payloadCryptoService.decryptPayload(encData);
                decryptedBody = objectMapper.readTree(decryptedPayload);
            } catch (Exception ex) {
                ObjectNode error = objectMapper.createObjectNode();
                error.put("message", "Invalid encrypted request payload");
                error.put("error", ex.getMessage() == null ? "Bad request" : ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(encryptIfNeeded(error, true));
            }
        } else if (payloadRequired) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", "encData is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(encryptIfNeeded(error, true));
        }

        JsonNode sanitizedBody = stripTokenField(decryptedBody);
        requestPayload = sanitizedBody == null ? null : sanitizedBody.toString();

        String requestId = extractRequestId(sanitizedBody);

        try {
            ApiResult result = service.runApi(apiName, sanitizedBody);
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

            JsonNode outgoing = encryptIfNeeded(customizedResponseJson, encryptedMode);
            return ResponseEntity.status(statusCode).body(outgoing);
        } catch (Exception ex) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", "Failed to run api");
            error.put("error", ex.getMessage() == null ? "Internal server error" : ex.getMessage());

            auditService.writeAudit(requestId, requestPayload, error.toString(), "ERROR");

            HttpStatus status = isUnknownApiName(ex) ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
            JsonNode outgoing = encryptIfNeeded(error, encryptedMode);
            return ResponseEntity.status(status).body(outgoing);
        }
    }

    private boolean hasEncData(JsonNode body) {
        if (body == null || body.isNull()) {
            return false;
        }
        return extractEncData(body) != null;
    }

    private String extractEncData(JsonNode body) {
        if (body == null || body.isNull()) {
            return null;
        }

        JsonNode encData = body.get("encData");
        if (encData != null && encData.isValueNode() && !encData.asText().isBlank()) {
            return encData.asText();
        }

        JsonNode encryptedData = body.get("encryptedData");
        if (encryptedData != null && encryptedData.isValueNode() && !encryptedData.asText().isBlank()) {
            return encryptedData.asText();
        }

        return null;
    }

    private JsonNode stripTokenField(JsonNode body) {
        if (!(body instanceof ObjectNode objectNode)) {
            return body;
        }

        ObjectNode copy = objectNode.deepCopy();
        copy.remove("token");
        return copy;
    }

    private JsonNode encryptIfNeeded(JsonNode plainJson, boolean encryptedMode) {
        if (!encryptedMode) {
            return plainJson;
        }

        try {
            String plain = plainJson == null ? "{}" : plainJson.toString();
            String encrypted = payloadCryptoService.encryptPayload(plain);
            ObjectNode envelope = objectMapper.createObjectNode();
            envelope.put("encData", encrypted);
            return envelope;
        } catch (Exception ex) {
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("message", "Failed to encrypt response");
            return fallback;
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
