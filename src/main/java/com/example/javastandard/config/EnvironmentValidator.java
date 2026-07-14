package com.example.javastandard.config;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentValidator {
    private static final Set<String> SAME_SITE_VALUES =
            new HashSet<String>(Arrays.asList("lax", "strict", "none"));

    private final AppProperties properties;
    private final Environment environment;

    public EnvironmentValidator(
            AppProperties properties,
            Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        if (properties.getJwtSecret() == null || properties.getJwtSecret().trim().length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters.");
        }
        String sameSite = properties.getCookieSameSite() == null
                ? ""
                : properties.getCookieSameSite().toLowerCase(Locale.ROOT);
        if (!SAME_SITE_VALUES.contains(sameSite)) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE must be Lax, Strict, or None.");
        }
        if ("none".equals(sameSite) && !properties.isCookieSecure()) {
            throw new IllegalStateException("SameSite=None requires AUTH_COOKIE_SECURE=true.");
        }
        String path = properties.getDatabasePath();
        if (path == null || path.trim().isEmpty()
                || path.startsWith("jdbc:") || path.startsWith("postgres:")
                || path.startsWith("file:")) {
            throw new IllegalStateException("DATABASE_URL must be a SQLite file path.");
        }
        if (isProduction()) {
            if (properties.getJwtSecret().toLowerCase(Locale.ROOT).contains("development")) {
                throw new IllegalStateException("Development JWT secret is not allowed in production.");
            }
            if (!properties.isCookieSecure()) {
                throw new IllegalStateException("AUTH_COOKIE_SECURE=true is required in production.");
            }
            String corsOrigins = properties.getCorsOrigins();
            if (corsOrigins == null || corsOrigins.trim().isEmpty()) {
                throw new IllegalStateException("CORS_ORIGINS is required in production.");
            }
            if (Arrays.stream(corsOrigins.split("\\s*,\\s*"))
                    .anyMatch("*"::equals)) {
                throw new IllegalStateException("Wildcard CORS is not allowed in production.");
            }
        }
        File database = new File(path);
        File parent = database.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Cannot create SQLite database directory.");
        }
    }

    private boolean isProduction() {
        for (String profile : environment.getActiveProfiles()) {
            if ("production".equalsIgnoreCase(profile)) return true;
        }
        return false;
    }
}
