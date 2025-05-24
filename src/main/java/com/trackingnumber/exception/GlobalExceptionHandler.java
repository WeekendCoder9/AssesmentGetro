package com.trackingnumber.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationExceptions(
            WebExchangeBindException ex) {
        logger.warn("Validation error occurred", ex);

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = createErrorResponse(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                errors
        );

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    @ExceptionHandler(DuplicateTrackingNumberException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleDuplicateTrackingNumber(
            DuplicateTrackingNumberException ex) {
        logger.error("Duplicate tracking number generated", ex);

        Map<String, Object> response = createErrorResponse(
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                null
        );

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(response));
    }

    @ExceptionHandler(TrackingNumberException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleTrackingNumberException(
            TrackingNumberException ex) {
        logger.error("Tracking number generation error", ex);

        Map<String, Object> response = createErrorResponse(
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);

        Map<String, Object> response = createErrorResponse(
                "Internal server error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }

    private Map<String, Object> createErrorResponse(String message, int status, Object details) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status);
        response.put("error", message);
        if (details != null) {
            response.put("details", details);
        }
        return response;
    }
}
