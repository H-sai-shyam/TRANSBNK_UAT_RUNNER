package com.example.transbnk_uat_runner.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TokenGenerateRequest {

	@NotBlank(message = "Transaction user ID is required")
	@JsonProperty("transaction_userid")
	@JsonAlias({"transactionUserId"})
	private String transactionUserId;

	@NotBlank(message = "Transaction merchant ID is required")
	@JsonProperty("transaction_merchantid")
	@JsonAlias({"transactionMerchantId"})
	private String transactionMerchantId;

	@NotBlank(message = "Client ID is required")
	@JsonProperty("client_Id")
	@JsonAlias({"clientId", "client_id"})
	private String clientId;

	@NotBlank(message = "Transaction timestamp is required")
	@Pattern(
			regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}",
			message = "Timestamp must be in format: yyyy-MM-dd HH:mm:ss"
	)
	@JsonProperty("transaction_timestamp")
	@JsonAlias({"transactionTimestamp"})
	private String transactionTimestamp;

	@NotBlank(message = "Processor is required")
	private String processor;

	public String getTransactionUserId() {
		return transactionUserId;
	}

	public void setTransactionUserId(String transactionUserId) {
		this.transactionUserId = transactionUserId;
	}

	public String getTransactionMerchantId() {
		return transactionMerchantId;
	}

	public void setTransactionMerchantId(String transactionMerchantId) {
		this.transactionMerchantId = transactionMerchantId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getTransactionTimestamp() {
		return transactionTimestamp;
	}

	public void setTransactionTimestamp(String transactionTimestamp) {
		this.transactionTimestamp = transactionTimestamp;
	}

	public String getProcessor() {
		return processor;
	}

	public void setProcessor(String processor) {
		this.processor = processor;
	}
}

