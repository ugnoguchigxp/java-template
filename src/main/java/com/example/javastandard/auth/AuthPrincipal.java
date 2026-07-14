package com.example.javastandard.auth;

public class AuthPrincipal {
    private final String userId;
    private final String email;
    private final String role;

    public AuthPrincipal(String userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
