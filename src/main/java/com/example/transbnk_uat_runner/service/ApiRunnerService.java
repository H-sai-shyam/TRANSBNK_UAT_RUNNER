package com.example.transbnk_uat_runner.service;

import com.example.transbnk_uat_runner.model.ApiResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class ApiRunnerService {

    @Value("${transbnk.base-url}")
    private String baseUrl;

    @Value("${transbnk.api-key}")
    private String apiKey;

    @Value("${transbnk.bank-account-validation-api-key:${transbnk.api-key}}")
    private String bankAccountValidationApiKey;

    @Value("${transbnk.entity-id}")
    private String entityId;

    @Value("${transbnk.program-id}")
    private String programId;

    @Value("${transbnk.nach-program-id:${transbnk.program-id}}")
    private String nachProgramId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;

    private static final Logger log =
            LoggerFactory.getLogger(ApiRunnerService.class);

    public ApiRunnerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ================= MAIN METHOD =================
    public ApiResult runApi(String apiName) throws Exception {

        log.info(" Running API: {}", apiName);

        // Load request JSON
        ClassPathResource resource =
                new ClassPathResource("api-requests/" + apiName + ".json");

        InputStream is = resource.getInputStream();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                .replace("{{entityId}}", entityId)
                .replace("{{programId}}", programId)
                .replace("{{nachProgramId}}", nachProgramId);

        log.debug(" Request body: {}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKeyFor(apiName));

        log.debug(" Headers: {}", headers);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ApiResult result = new ApiResult();
        result.setApiName(apiName);
        result.setRequest(mapper.readTree(body));
        String endpointPath = endpoint(apiName);
        String url = baseUrl + endpointPath;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            int httpStatus = response.getStatusCode().value();
            result.setStatusCode(httpStatus);
            classifyHttpStatus(httpStatus, result);

            String responseBody =
                    response.getBody() == null ? "{}" : response.getBody();

            result.setResponse(mapper.readTree(responseBody));
            classifyBusinessStatus(responseBody, result);

            log.info(" HTTP {} | {}", httpStatus, apiName);
            log.debug(" Response body: {}", responseBody);

        } catch (HttpStatusCodeException ex) {

            int httpStatus = ex.getStatusCode().value();
            result.setStatusCode(httpStatus);
            classifyHttpStatus(httpStatus, result);

            String errorBody =
                    ex.getResponseBodyAsString() == null
                            ? "{}"
                            : ex.getResponseBodyAsString();

            result.setResponse(mapper.readTree(errorBody));
            result.setBusinessStatus("HTTP_ERROR");

            log.error(" HTTP {} | {}", httpStatus, apiName);
            log.error(" Error body: {}", errorBody);
        }

        log.info(
            " Classification | httpStatus={} | httpCategory={} | businessStatus={}",
            result.getStatusCode(),
            result.getStatusCategory(),
            result.getBusinessStatus()
        );

        saveToDatabase(apiName, endpointPath, url, result);
        save(apiName, result);
        return result;
    }

    // ================= HELPERS =================
    private void classifyHttpStatus(int status, ApiResult result) {
        if (status >= 200 && status < 300) {
            result.setStatusCategory("SUCCESS");
        } else if (status >= 400 && status < 500) {
            result.setStatusCategory("CLIENT_ERROR");
        } else if (status >= 500) {
            result.setStatusCategory("SERVER_ERROR");
        } else {
            result.setStatusCategory("UNKNOWN");
        }
    }

    private void classifyBusinessStatus(String body, ApiResult result)
            throws Exception {

        if (body == null || body.isBlank()) {
            result.setBusinessStatus("EMPTY_RESPONSE");
            return;
        }

        JsonNode json = mapper.readTree(body);

        if (json.has("result_code")) {
            int code = json.get("result_code").asInt();
            if (code == 101) {
                result.setBusinessStatus("SUCCESS");
            } else if (code == 102 || code == 103) {
                result.setBusinessStatus("NO_RECORD");
            } else {
                result.setBusinessStatus("FAILED");
            }
            return;
        }

        if (json.has("statuscode")) {
            int sc = json.get("statuscode").asInt();
            result.setBusinessStatus(sc == 200 ? "SUCCESS" : "FAILED");
            return;
        }

        if (json.has("status")) {
            result.setBusinessStatus(json.get("status").asText());
            return;
        }

        result.setBusinessStatus("UNKNOWN");
    }

    private void save(String apiName, ApiResult result) throws Exception {
        File dir = new File("responses");
        if (!dir.exists()) {
            dir.mkdirs();
            log.info(" Created responses directory");
        }

        File file = new File(dir, apiName + "-response.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, result);

        log.info(" Response saved: {}", file.getAbsolutePath());
    }

    private String apiKeyFor(String apiName) {
        if ("bank-account-validation".equals(apiName)) {
            return bankAccountValidationApiKey;
        }
        return apiKey;
    }

    private void saveToDatabase(
            String apiName,
            String endpointPath,
            String url,
            ApiResult result
    ) {
        if (!"bank-account-validation".equals(apiName)) {
            return;
        }

        String sql = """
                INSERT INTO bank_account_validation_log (
                    endpoint_path,
                    full_url,
                    http_status,
                    http_category,
                    business_status,
                    entity_id,
                    program_id,
                    request_id,
                    cust_name,
                    cust_ifsc,
                    cust_acct_no,
                    tracking_ref_no,
                    txn_type,
                    tb_status_code,
                    tb_status,
                    ac_validation_status,
                    message,
                    response_id,
                    name_at_bank,
                    bank_code,
                    method_used,
                    utr,
                    request_json,
                    response_json
                )
                VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?
                )
                """;

        try {
            JsonNode requestJson = result.getRequest();
            JsonNode responseJson = result.getResponse();

            jdbcTemplate.update(
                    sql,
                    endpointPath,
                    url,
                    result.getStatusCode(),
                    result.getStatusCategory(),
                    result.getBusinessStatus(),
                    jsonText(requestJson, "entityId"),
                    jsonText(requestJson, "programId"),
                    jsonText(requestJson, "requestId"),
                    jsonText(requestJson, "custName"),
                    jsonText(requestJson, "custIfsc"),
                    jsonText(requestJson, "custAcctNo"),
                    jsonText(requestJson, "trackingRefNo"),
                    jsonText(requestJson, "txnType"),
                    jsonText(responseJson, "statusCode"),
                    jsonText(responseJson, "status"),
                    jsonText(responseJson, "acValidationStatus"),
                    jsonText(responseJson, "message"),
                    jsonText(responseJson, "responseId"),
                    jsonText(responseJson, "nameAtBank"),
                    jsonText(responseJson, "bankCode"),
                    jsonText(responseJson, "methodUsed"),
                    jsonText(responseJson, "utr"),
                    requestJson == null ? null : requestJson.toString(),
                    responseJson == null ? null : responseJson.toString()
            );
            log.info(" DB saved | apiName={}", apiName);
        } catch (Exception e) {
            log.error(" DB save failed | apiName={}", apiName, e);
        }
    }

    private static String jsonText(JsonNode json, String fieldName) {
        if (json == null) {
            return null;
        }
        JsonNode value = json.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isValueNode()) {
            return value.asText();
        }
        return value.toString();
    }

    private String endpoint(String apiName) {
        return switch (apiName) {
            case "aadhaar-validation" -> "/basic-aadhaar-validation";
            case "bank-account-validation" -> "/bank-validateacct";
            case "vpa-validation" -> "/validate-vpa";

            case "docuflow-create" -> "/docuflow-1call";
            case "docuflow-status" -> "/docuflow-status";
            case "docuflow-resend" -> "/docuflow-resend";
            case "docuflow-cancel" -> "/docuflow-cancel";

            case "nach-mandate-create" -> "/nach-mandate-request";
            case "nach-status" -> "/nach-status";

            case "upi-validate-vpa" -> "/upi/validate-vpa";
            case "upi-mandate-create" -> "/upi/mandate/create";

            case "payout-create" -> "/payout";

            default -> throw new RuntimeException("Unknown apiName: " + apiName);
        };
    }
}
