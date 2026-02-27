package com.example.transbnk_uat_runner.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class TokenValidateRequest {

	@NotBlank(message = "Token is required")
	private String token;

	@JsonAlias({"client_Id", "client_id", "clientid"})
	private String clientId;

	private String processor;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getProcessor() {
		return processor;
	}

	public void setProcessor(String processor) {
		this.processor = processor;
	}
}
