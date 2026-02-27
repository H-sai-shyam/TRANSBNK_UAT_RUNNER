package com.example.transbnk_uat_runner.wrapper.dto;

public class TokenGenerateResponse {

	private String token;

	public TokenGenerateResponse() {
	}

	public TokenGenerateResponse(String token) {
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}

