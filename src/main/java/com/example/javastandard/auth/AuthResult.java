package com.example.javastandard.auth;

public class AuthResult {
    private final String accessToken;
    private final String refreshToken;
    private final AuthUser user;

    public AuthResult(String accessToken, String refreshToken, AuthUser user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public AuthUser getUser() { return user; }
}
