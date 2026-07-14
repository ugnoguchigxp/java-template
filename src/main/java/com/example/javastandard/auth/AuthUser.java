package com.example.javastandard.auth;

import com.example.javastandard.db.model.UserRecord;

public record AuthUser(String id, String email, String displayName, String role, boolean active) {
    public AuthUser(UserRecord record) {
        this(record.getId(), record.getEmail(), record.getDisplayName(),
                "admin".equals(record.getRole()) ? "admin" : "member", record.isActive());
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
}
