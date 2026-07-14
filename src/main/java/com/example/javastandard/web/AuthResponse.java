package com.example.javastandard.web;

public class AuthResponse {
    private final SessionUser user;

    public AuthResponse(SessionUser user) {
        this.user = user;
    }

    public SessionUser getUser() { return user; }
}
