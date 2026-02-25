package com.example.transbnk_uat_runner.model;

import com.fasterxml.jackson.databind.JsonNode;

public class ApiResult {

    private String apiName;
    private int statusCode;          // HTTP status
    private String statusCategory;   // SUCCESS / CLIENT_ERROR / SERVER_ERROR
    private String businessStatus;   // SUCCESS / FAILED / NO_RECORD / HTTP_ERROR
    private JsonNode request;
    private JsonNode response;

    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getStatusCategory() { return statusCategory; }
    public void setStatusCategory(String statusCategory) {
        this.statusCategory = statusCategory;
    }

    public String getBusinessStatus() { return businessStatus; }
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    public JsonNode getRequest() { return request; }
    public void setRequest(JsonNode request) { this.request = request; }

    public JsonNode getResponse() { return response; }
    public void setResponse(JsonNode response) { this.response = response; }
}
