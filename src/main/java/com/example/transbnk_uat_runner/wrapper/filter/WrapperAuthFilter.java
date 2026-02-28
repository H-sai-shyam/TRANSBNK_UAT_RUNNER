package com.example.transbnk_uat_runner.wrapper.filter;

import com.example.transbnk_uat_runner.wrapper.service.WrapperAuditService;
import com.example.transbnk_uat_runner.wrapper.service.WrapperPayloadCryptoService;
import com.example.transbnk_uat_runner.wrapper.service.WrapperTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
	private final WrapperPayloadCryptoService payloadCryptoService;
	private final ObjectMapper objectMapper;

	private final boolean payloadRequired;

	public WrapperAuthFilter(
			WrapperTokenService tokenService,
			WrapperAuditService auditService,
			WrapperPayloadCryptoService payloadCryptoService,
			ObjectMapper objectMapper,
			@Value("${wrapper.payload-required:false}") boolean payloadRequired
	) {
		this.tokenService = tokenService;
		this.auditService = auditService;
		this.payloadCryptoService = payloadCryptoService;
		this.objectMapper = objectMapper;
		this.payloadRequired = payloadRequired;
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
		if (token != null && !token.isBlank()) {
			if (!tokenService.validateToken(token)) {
				writeUnauthorizedWithAudit(request, response, "Invalid or expired token");
				return;
			}

			filterChain.doFilter(request, response);
			return;
		}

		CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
		String requestPayload = safeReadBody(cachedRequest);
		token = extractTokenFromBody(requestPayload);

		if (token == null || token.isBlank()) {
			writeUnauthorizedWithAudit(cachedRequest, response, "Authorization header or body token is required");
			return;
		}

		if (!tokenService.validateToken(token)) {
			writeUnauthorizedWithAudit(cachedRequest, response, "Invalid or expired token");
			return;
		}

		filterChain.doFilter(cachedRequest, response);
	}

	private void writeUnauthorizedWithAudit(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
		String requestPayload = safeReadBody(request);

		String encData = tryExtractEncData(requestPayload);
		boolean encryptedMode = payloadRequired || (encData != null && !encData.isBlank());

		String auditRequestPayload = requestPayload;
		String requestId = extractRequestId(auditRequestPayload);
		String plainResponsePayload = "{\"message\":\"" + sanitize(message) + "\"}";

		auditService.writeAudit(requestId, auditRequestPayload, plainResponsePayload, "TOKEN_INVALID");

		String responsePayload = plainResponsePayload;
		if (encryptedMode) {
			try {
				String encrypted = payloadCryptoService.encryptPayload(plainResponsePayload);
				responsePayload = "{\"encData\":\"" + sanitize(encrypted) + "\"}";
			} catch (Exception ignored) {
				// fall back to plain response payload
			}
		}

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

	private String extractTokenFromBody(String requestPayload) {
		if (requestPayload == null || requestPayload.isBlank()) {
			return null;
		}

		try {
			JsonNode json = objectMapper.readTree(requestPayload);
			JsonNode token = json.get("token");
			if (token != null && token.isValueNode() && !token.asText().isBlank()) {
				return token.asText().trim();
			}
		} catch (Exception ignored) {
			// ignore
		}

		return null;
	}

	private String tryExtractEncData(String requestPayload) {
		if (requestPayload == null || requestPayload.isBlank()) {
			return null;
		}

		try {
			JsonNode json = objectMapper.readTree(requestPayload);
			JsonNode encData = json.get("encData");
			if (encData != null && encData.isValueNode() && !encData.asText().isBlank()) {
				return encData.asText();
			}

			JsonNode encryptedData = json.get("encryptedData");
			if (encryptedData != null && encryptedData.isValueNode() && !encryptedData.asText().isBlank()) {
				return encryptedData.asText();
			}
		} catch (Exception ignored) {
			// ignore
		}

		return null;
	}
}
