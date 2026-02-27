package com.example.transbnk_uat_runner.wrapper.dto;

public class TokenValidateResponse {

	private boolean valid;

	public TokenValidateResponse() {
	}

	public TokenValidateResponse(boolean valid) {
		this.valid = valid;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
}

