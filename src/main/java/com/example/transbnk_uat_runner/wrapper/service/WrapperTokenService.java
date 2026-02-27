package com.example.transbnk_uat_runner.wrapper.service;

import com.example.transbnk_uat_runner.wrapper.dto.TokenGenerateRequest;
import com.example.transbnk_uat_runner.wrapper.dto.TokenValidateRequest;
import com.example.transbnk_uat_runner.wrapper.util.AesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class WrapperTokenService {

	private static final Logger log = LoggerFactory.getLogger(WrapperTokenService.class);

	private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final JdbcTemplate jdbcTemplate;

	@Value("${wrapper.transaction-password}")
	private String transactionPassword;

	@Value("${wrapper.transaction-aes-key}")
	private String transactionAesKey;

	@Value("${wrapper.transaction-iv}")
	private String transactionIv;

	@Value("${wrapper.token-expiry-minutes:15}")
	private long tokenExpiryMinutes;

	public WrapperTokenService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public String generateToken(TokenGenerateRequest request) throws Exception {
		if (request == null) {
			throw new IllegalArgumentException("Request is required");
		}

		String processor = normalizeProcessor(request.getProcessor());
		LocalDateTime txnTimestamp = LocalDateTime.parse(request.getTransactionTimestamp(), TS_FORMATTER);

		Optional<MasterTransaction> existing = findLatestByClientIdAndTransactionTimestampAndProcessor(
				request.getClientId(),
				txnTimestamp,
				processor
		);

		if (existing.isPresent()) {
			long minutesPassed = Duration.between(txnTimestamp, LocalDateTime.now()).toMinutes();
			if (minutesPassed < tokenExpiryMinutes) {
				log.info("Token already exists (within expiry) | Returning existing token");
				return existing.get().transactionToken();
			}
		}

		String normalizedTs = txnTimestamp.format(TS_FORMATTER);
		String raw = String.valueOf(request.getTransactionUserId())
				+ String.valueOf(request.getTransactionMerchantId())
				+ String.valueOf(transactionPassword)
				+ normalizedTs
				+ processor;

		String encryptedToken = AesUtil.encrypt(raw, transactionAesKey, transactionIv);

		String orderReference = "TXN"
				+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
				+ UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);

		LocalDateTime now = LocalDateTime.now();
		String id = UUID.randomUUID().toString();

		String insertSql = """
				INSERT INTO master_transactions (
					id,
					order_reference,
					transaction_token,
					transaction_timestamp,
					transaction_userid,
					transaction_merchantid,
					client_id,
					processor,
					initiated_at,
					completed_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		jdbcTemplate.update(
				insertSql,
				id,
				orderReference,
				encryptedToken,
				Timestamp.valueOf(txnTimestamp),
				request.getTransactionUserId(),
				request.getTransactionMerchantId(),
				request.getClientId(),
				processor,
				Timestamp.valueOf(now),
				Timestamp.valueOf(now)
		);

		log.info("Token generated | clientId={} processor={} orderReference={}", request.getClientId(), processor, orderReference);
		return encryptedToken;
	}

	public boolean validateToken(TokenValidateRequest request) {
		if (request == null) {
			return false;
		}
		return validateToken(request.getToken(), request.getClientId(), request.getProcessor());
	}

	public boolean validateToken(String token) {
		return validateToken(token, null, null);
	}

	public boolean validateToken(String token, String clientId, String processor) {
		try {
			if (token == null || token.isBlank()) {
				return false;
			}

			MasterTransaction masterTxn = findByTransactionToken(token)
					.orElseThrow(() -> new IllegalArgumentException("Invalid or unknown transaction token"));

			if (clientId != null && !clientId.isBlank() && !masterTxn.clientId().equals(clientId)) {
				throw new IllegalArgumentException("Token does not belong to this client");
			}

			if (processor != null && !processor.isBlank() && !masterTxn.processor().equalsIgnoreCase(processor)) {
				throw new IllegalArgumentException("Token processor mismatch");
			}

			LocalDateTime ts = masterTxn.transactionTimestamp();
			if (ts == null) {
				throw new IllegalStateException("Missing merchant transaction timestamp");
			}

			long minutesPassed = Duration.between(ts, LocalDateTime.now()).toMinutes();
			if (minutesPassed >= tokenExpiryMinutes) {
				throw new IllegalArgumentException("Transaction token expired");
			}

			String expectedRaw = String.valueOf(masterTxn.transactionUserId())
					+ String.valueOf(masterTxn.transactionMerchantId())
					+ String.valueOf(transactionPassword)
					+ ts.format(TS_FORMATTER)
					+ masterTxn.processor();

			String decryptedRaw = AesUtil.decrypt(token, transactionAesKey, transactionIv);
			if (!expectedRaw.equals(decryptedRaw)) {
				throw new IllegalArgumentException("Invalid transaction token (payload mismatch)");
			}

			return true;
		} catch (Exception ex) {
			log.warn("Token validation failed: {}", ex.getMessage());
			return false;
		}
	}

	private String normalizeProcessor(String processor) {
		if (processor == null || processor.isBlank()) {
			throw new IllegalArgumentException("Processor is required");
		}
		return processor.trim().toUpperCase(Locale.ROOT);
	}

	private Optional<MasterTransaction> findLatestByClientIdAndTransactionTimestampAndProcessor(
			String clientId,
			LocalDateTime transactionTimestamp,
			String processor
	) {
		String sql = """
				SELECT
					id,
					order_reference,
					transaction_token,
					transaction_timestamp,
					transaction_userid,
					transaction_merchantid,
					client_id,
					processor,
					initiated_at,
					completed_at
				FROM master_transactions
				WHERE client_id = ?
				  AND transaction_timestamp = ?
				  AND processor = ?
				ORDER BY initiated_at DESC
				LIMIT 1
				""";

		try {
			MasterTransaction row = jdbcTemplate.queryForObject(
					sql,
					(rs, rowNum) -> new MasterTransaction(
							rs.getString("id"),
							rs.getString("order_reference"),
							rs.getString("transaction_token"),
							toLdt(rs.getTimestamp("transaction_timestamp")),
							rs.getString("transaction_userid"),
							rs.getString("transaction_merchantid"),
							rs.getString("client_id"),
							rs.getString("processor"),
							toLdt(rs.getTimestamp("initiated_at")),
							toLdt(rs.getTimestamp("completed_at"))
					),
					clientId,
					Timestamp.valueOf(transactionTimestamp),
					processor
			);
			return Optional.ofNullable(row);
		} catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}
	}

	private Optional<MasterTransaction> findByTransactionToken(String token) {
		String sql = """
				SELECT
					id,
					order_reference,
					transaction_token,
					transaction_timestamp,
					transaction_userid,
					transaction_merchantid,
					client_id,
					processor,
					initiated_at,
					completed_at
				FROM master_transactions
				WHERE transaction_token = ?
				LIMIT 1
				""";

		try {
			MasterTransaction row = jdbcTemplate.queryForObject(
					sql,
					(rs, rowNum) -> new MasterTransaction(
							rs.getString("id"),
							rs.getString("order_reference"),
							rs.getString("transaction_token"),
							toLdt(rs.getTimestamp("transaction_timestamp")),
							rs.getString("transaction_userid"),
							rs.getString("transaction_merchantid"),
							rs.getString("client_id"),
							rs.getString("processor"),
							toLdt(rs.getTimestamp("initiated_at")),
							toLdt(rs.getTimestamp("completed_at"))
					),
					token
			);
			return Optional.ofNullable(row);
		} catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}
	}

	private static LocalDateTime toLdt(Timestamp ts) {
		return ts == null ? null : ts.toLocalDateTime();
	}

	private record MasterTransaction(
			String id,
			String orderReference,
			String transactionToken,
			LocalDateTime transactionTimestamp,
			String transactionUserId,
			String transactionMerchantId,
			String clientId,
			String processor,
			LocalDateTime initiatedAt,
			LocalDateTime completedAt
	) {
	}
}

