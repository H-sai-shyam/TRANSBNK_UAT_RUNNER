package com.example.transbnk_uat_runner.service;

import com.example.transbnk_uat_runner.model.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.File;


@Service
public class ApiRunnerService {

    @Value("${transbnk.base-url}")
    private String baseUrl;

    @Value("${transbnk.api-key}")
    private String apiKey;

    @Value("${transbnk.entity-id}")
    private String entityId;

    @Value("${transbnk.program-id}")
    private String programId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiResult runApi(String apiName) throws Exception {

    // Load JSON request
    ClassPathResource resource =
            new ClassPathResource("api-requests/" + apiName + ".json");

    InputStream is = resource.getInputStream();
    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8)
            .replace("{{entityId}}", entityId)
            .replace("{{programId}}", programId);

    // Headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-key", apiKey);

    HttpEntity<String> request = new HttpEntity<>(body, headers);

    ResponseEntity<String> response;

    try {
        response = restTemplate.exchange(
                baseUrl + endpoint(apiName),
                HttpMethod.POST,
                request,
                String.class
        );
    } catch (HttpStatusCodeException ex) {
        ApiResult errorResult = new ApiResult();
        errorResult.setApiName(apiName);
        errorResult.setRequest(mapper.readTree(body));
        errorResult.setStatusCode(ex.getStatusCode().value());
        errorResult.setResponse(
                mapper.readTree(
                        ex.getResponseBodyAsString() == null
                                ? "{}"
                                : ex.getResponseBodyAsString()
                )
        );
        save(apiName, errorResult);
        return errorResult;
    }

    ApiResult result = new ApiResult();
    result.setApiName(apiName);
    result.setRequest(mapper.readTree(body));
    result.setStatusCode(response.getStatusCode().value());

    if (response.getBody() != null && !response.getBody().isBlank()) {
        result.setResponse(mapper.readTree(response.getBody()));
    } else {
        result.setResponse(mapper.readTree("{}"));
    }

    save(apiName, result);
    return result;
}


    private void save(String apiName, ApiResult result) throws Exception {
        File dir = new File("responses");
        dir.mkdirs();
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File(dir, apiName + "-response.json"), result);
    }

    private String endpoint(String apiName) {
        return switch (apiName) {
            case "aadhaar-validation" -> "/basic-aadhaar-validation";
            case "bank-account-validation" -> "/validate-acct-3";
            case "vpa-validation" -> "/validate-vpa";

            case "docuflow-create" -> "/docuflow-1call";
            case "docuflow-status" -> "/docuflow-status";
            case "docuflow-resend" -> "/docuflow-resend";
            case "docuflow-cancel" -> "/docuflow-cancel";

            case "nach-mandate-create" -> "/nach-mandate";
            case "nach-status" -> "/nach-status";

            case "upi-validate-vpa" -> "/upi/validate-vpa";
            case "upi-mandate-create" -> "/upi/mandate/create";

            case "payout-create" -> "/payout";

            default -> throw new RuntimeException("Unknown apiName: " + apiName);
        };
    }
}
