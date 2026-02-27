package com.example.transbnk_uat_runner.wrapper.controller;

import com.example.transbnk_uat_runner.wrapper.dto.TokenGenerateRequest;
import com.example.transbnk_uat_runner.wrapper.dto.TokenGenerateResponse;
import com.example.transbnk_uat_runner.wrapper.dto.TokenValidateRequest;
import com.example.transbnk_uat_runner.wrapper.dto.TokenValidateResponse;
import com.example.transbnk_uat_runner.wrapper.service.WrapperTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/token")
public class TokenController {

	private final WrapperTokenService tokenService;

	public TokenController(WrapperTokenService tokenService) {
		this.tokenService = tokenService;
	}

	@PostMapping("/generate")
	public ResponseEntity<TokenGenerateResponse> generate(@RequestBody @Valid TokenGenerateRequest request) throws Exception {
		String token = tokenService.generateToken(request);
		return ResponseEntity.ok(new TokenGenerateResponse(token));
	}

	@PostMapping("/validate")
	public ResponseEntity<TokenValidateResponse> validate(@RequestBody @Valid TokenValidateRequest request) {
		boolean valid = tokenService.validateToken(request);
		return ResponseEntity.ok(new TokenValidateResponse(valid));
	}
}

