package com.example.javastandard.web;

import java.util.Collections;
import java.util.Map;

public class ErrorResponse {
    private final String message;
    private final String code;
    private final Map<String, String> fieldErrors;

    public ErrorResponse(String message, String code) {
        this(message, code, Collections.<String, String>emptyMap());
    }

    public ErrorResponse(String message, String code, Map<String, String> fieldErrors) {
        this.message = message;
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

    public String getMessage() { return message; }
    public String getCode() { return code; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
