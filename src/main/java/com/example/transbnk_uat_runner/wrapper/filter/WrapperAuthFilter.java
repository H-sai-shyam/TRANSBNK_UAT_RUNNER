package com.example.transbnk_uat_runner.wrapper.filter;

import com.example.transbnk_uat_runner.wrapper.service.WrapperAuditService;
import com.example.transbnk_uat_runner.wrapper.service.WrapperTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Component
public class WrapperAuthFilter extends OncePerRequestFilter {

	private final WrapperTokenService tokenService;
	private final WrapperAuditService auditService;
	private final ObjectMapper objectMapper;

	public WrapperAuthFilter(WrapperTokenService tokenService, WrapperAuditService auditService, ObjectMapper objectMapper) {
		this.tokenService = tokenService;
		this.auditService = auditService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String method = request.getMethod();
		if (method != null && method.equalsIgnoreCase("OPTIONS")) {
			return true;
		}

		String path = request.getRequestURI();
		if (path == null || path.isBlank()) {
			return true;
		}

		if (path.startsWith("/api/v1/token")) {
			return true;
		}

		if (path.startsWith("/error") || path.startsWith("/actuator")) {
			return true;
		}

		return !path.startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String token = extractBearerToken(request.getHeader("Authorization"));
		if (token == null || token.isBlank()) {
			writeUnauthorizedWithAudit(request, response, "Authorization header is required");
			return;
		}

		if (!tokenService.validateToken(token)) {
			writeUnauthorizedWithAudit(request, response, "Invalid or expired token");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void writeUnauthorizedWithAudit(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
		String requestPayload = safeReadBody(request);
		String requestId = extractRequestId(requestPayload);
		String responsePayload = "{\"message\":\"" + sanitize(message) + "\"}";

		auditService.writeAudit(requestId, requestPayload, responsePayload, "TOKEN_INVALID");

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(responsePayload);
	}

	private String safeReadBody(HttpServletRequest request) {
		try {
			return StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			return null;
		}
	}

	private String extractRequestId(String requestPayload) {
		if (requestPayload == null || requestPayload.isBlank()) {
			return UUID.randomUUID().toString();
		}

		try {
			JsonNode json = objectMapper.readTree(requestPayload);
			JsonNode requestId = json.get("requestId");
			if (requestId != null && requestId.isValueNode() && !requestId.asText().isBlank()) {
				return requestId.asText();
			}

			JsonNode requestIdSnake = json.get("request_id");
			if (requestIdSnake != null && requestIdSnake.isValueNode() && !requestIdSnake.asText().isBlank()) {
				return requestIdSnake.asText();
			}
		} catch (Exception ignored) {
			// ignore
		}

		return UUID.randomUUID().toString();
	}

	private static String extractBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || authorizationHeader.isBlank()) {
			return null;
		}

		String headerValue = authorizationHeader.trim();
		if (headerValue.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
			headerValue = headerValue.substring(7).trim();
		}

		if (headerValue.length() >= 2 && headerValue.startsWith("\"") && headerValue.endsWith("\"")) {
			headerValue = headerValue.substring(1, headerValue.length() - 1).trim();
		}

		return headerValue;
	}

	private static String sanitize(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}

