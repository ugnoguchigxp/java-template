package com.example.javastandard.auth;

import com.example.javastandard.db.model.UserRecord;

public class AuthUser {
    private final String id;
    private final String email;
    private final String displayName;
    private final String role;
    private final boolean active;

    public AuthUser(UserRecord record) {
        this.id = record.getId();
        this.email = record.getEmail();
        this.displayName = record.getDisplayName();
        this.role = "admin".equals(record.getRole()) ? "admin" : "member";
        this.active = record.isActive();
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
}
