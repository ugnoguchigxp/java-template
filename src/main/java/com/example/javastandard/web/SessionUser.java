package com.example.javastandard.web;

import com.example.javastandard.auth.AuthUser;

public record SessionUser(String id, String email, String displayName, String role) {
    public SessionUser(AuthUser user) {
        this(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
}
