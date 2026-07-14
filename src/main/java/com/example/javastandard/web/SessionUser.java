package com.example.javastandard.web;

import com.example.javastandard.auth.AuthUser;

public class SessionUser {
    private final String id;
    private final String email;
    private final String displayName;
    private final String role;

    public SessionUser(AuthUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
        this.role = user.getRole();
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
}
