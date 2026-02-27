package com.example.transbnk_uat_runner.wrapper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WrapperAuditService {

	private static final Logger log = LoggerFactory.getLogger(WrapperAuditService.class);

	private final JdbcTemplate jdbcTemplate;

	public WrapperAuditService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void writeAudit(String requestId, String requestPayload, String responsePayload, String status) {
		String sql = """
				INSERT INTO bank_validation_audit (
					request_id,
					request_payload,
					response_payload,
					status,
					created_at
				) VALUES (?, ?, ?, ?, NOW())
				""";

		try {
			jdbcTemplate.update(
					sql,
					requestId,
					truncate(requestPayload),
					truncate(responsePayload),
					status
			);
		} catch (Exception ex) {
			log.warn("Wrapper audit insert failed | requestId={} message={}", requestId, ex.getMessage());
		}
	}

	private static String truncate(String value) {
		if (value == null) {
			return null;
		}
		int max = 4000;
		if (value.length() <= max) {
			return value;
		}
		return value.substring(0, max);
	}
}

