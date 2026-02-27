package com.example.transbnk_uat_runner.wrapper.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		List<Map<String, String>> errors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(ApiExceptionHandler::toError)
				.toList();

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", "Validation failed");
		body.put("errors", errors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", "Invalid JSON request body");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", ex.getMessage() == null ? "Bad request" : ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	private static Map<String, String> toError(FieldError fieldError) {
		Map<String, String> error = new LinkedHashMap<>();
		error.put("field", fieldError.getField());
		error.put("message", fieldError.getDefaultMessage());
		return error;
	}
}

