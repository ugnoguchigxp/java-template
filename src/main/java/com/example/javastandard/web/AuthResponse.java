package com.example.javastandard.web;

public record AuthResponse(SessionUser user) {
    public SessionUser getUser() { return user; }
}
