package com.example.javastandard.web;

import com.example.javastandard.auth.AuthException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> auth(AuthException exception) {
        return ResponseEntityFactory.of(exception.getStatus(),
                new ErrorResponse(exception.getMessage(), exception.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> validation(
            MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntityFactory.of(HttpStatus.BAD_REQUEST,
                new ErrorResponse("Validation failed.", "VALIDATION_ERROR", fields));
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        return ResponseEntityFactory.of(HttpStatus.INTERNAL_SERVER_ERROR,
                new ErrorResponse("Internal server error.", "INTERNAL_ERROR"));
    }

    private static final class ResponseEntityFactory {
        private static <T> org.springframework.http.ResponseEntity<T> of(
                HttpStatus status, T body) {
            return new org.springframework.http.ResponseEntity<T>(body, status);
        }
    }
}
