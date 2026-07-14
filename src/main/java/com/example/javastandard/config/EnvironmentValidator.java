package com.example.javastandard.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import jakarta.annotation.PostConstruct;
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
        validateDuration(properties.getJwtAccessTtl(), "JWT_ACCESS_TTL");
        validateDuration(properties.getJwtRefreshTtl(), "JWT_REFRESH_TTL");
        if ("none".equals(sameSite) && !properties.isCookieSecure()) {
            throw new IllegalStateException("SameSite=None requires AUTH_COOKIE_SECURE=true.");
        }
        String url = properties.getDatabaseUrl();
        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("DATABASE_URL must be a PostgreSQL JDBC URL.");
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
    }

    private boolean isProduction() {
        for (String profile : environment.getActiveProfiles()) {
            if ("production".equalsIgnoreCase(profile)) return true;
        }
        return false;
    }

    private void validateDuration(String value, String name) {
        if (value == null || !value.matches("^\\d+[smhd]$")) {
            throw new IllegalStateException(name + " must be a positive duration such as 15m or 30d.");
        }
        try {
            long amount = Long.parseLong(value.substring(0, value.length() - 1));
            if (amount <= 0) throw new ArithmeticException();
            long seconds;
            switch (value.charAt(value.length() - 1)) {
                case 's': seconds = amount; break;
                case 'm': seconds = Math.multiplyExact(amount, 60L); break;
                case 'h': seconds = Math.multiplyExact(amount, 60L * 60L); break;
                case 'd': seconds = Math.multiplyExact(amount, 60L * 60L * 24L); break;
                default: throw new ArithmeticException();
            }
            Math.multiplyExact(seconds, 1000L);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalStateException(name + " must be a positive duration such as 15m or 30d.", exception);
        }
    }
}
