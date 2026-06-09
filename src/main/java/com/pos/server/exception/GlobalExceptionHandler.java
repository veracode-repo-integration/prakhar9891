package com.pos.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("ok", false, "message", "Access Denied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest().body(Map.of("ok", false, "errors", errors, "message", "Validation Failed"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: ", ex);
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (message != null) {
            if (message.contains("Forbidden")) {
                status = HttpStatus.FORBIDDEN;
            } else if (message.contains("Unauthorized")) {
                status = HttpStatus.UNAUTHORIZED;
            } else if (message.toLowerCase().contains("javax.persistence") || 
                       message.toLowerCase().contains("nonuniqueresult") || 
                       message.toLowerCase().contains("query did not return") ||
                       message.toLowerCase().contains("hibernate") ||
                       message.toLowerCase().contains("sql") ||
                       message.toLowerCase().contains("constraint")) {
                message = "An internal data conflict or database error occurred. Please try again.";
            }
        } else {
            message = "An unexpected error occurred.";
        }

        return ResponseEntity.status(status).body(Map.of("ok", false, "message", message));
    }

    @ExceptionHandler({
        org.springframework.dao.DataAccessException.class,
        javax.persistence.PersistenceException.class
    })
    public ResponseEntity<Map<String, Object>> handleDatabaseException(Exception ex) {
        log.error("Database or persistence exception: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("ok", false, "message", "A database conflict or validation error occurred. Please try again."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        log.error("Internal server error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("ok", false, "message", "Internal Server Error. Please contact support."));
    }
}
