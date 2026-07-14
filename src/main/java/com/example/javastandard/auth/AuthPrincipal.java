package com.example.javastandard.auth;

public record AuthPrincipal(String userId, String email, String role) {
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
