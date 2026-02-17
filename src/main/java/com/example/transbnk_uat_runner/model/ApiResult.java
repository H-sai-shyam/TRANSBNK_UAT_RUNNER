package com.example.transbnk_uat_runner.model;

import com.fasterxml.jackson.databind.JsonNode;

public class ApiResult {

    private String apiName;
    private JsonNode request;
    private JsonNode response;
    private int statusCode;

    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }

    public JsonNode getRequest() { return request; }
    public void setRequest(JsonNode request) { this.request = request; }

    public JsonNode getResponse() { return response; }
    public void setResponse(JsonNode response) { this.response = response; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
}
