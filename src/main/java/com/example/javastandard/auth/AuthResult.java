package com.example.javastandard.auth;

public record AuthResult(String accessToken, String refreshToken, AuthUser user) {
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public AuthUser getUser() { return user; }
}
