package com.example.javastandard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String databaseUrl = "jdbc:postgresql://127.0.0.1:5432/java_template";
    private String jwtSecret;
    private String jwtAccessTtl = "15m";
    private String jwtRefreshTtl = "30d";
    private String appUrl = "http://localhost:5173";
    private String corsOrigins = "http://localhost:5173";
    private boolean cookieSecure;
    private String cookieSameSite = "Lax";

    public String getDatabaseUrl() { return databaseUrl; }
    public void setDatabaseUrl(String databaseUrl) { this.databaseUrl = databaseUrl; }
    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public String getJwtAccessTtl() { return jwtAccessTtl; }
    public void setJwtAccessTtl(String jwtAccessTtl) { this.jwtAccessTtl = jwtAccessTtl; }
    public String getJwtRefreshTtl() { return jwtRefreshTtl; }
    public void setJwtRefreshTtl(String jwtRefreshTtl) { this.jwtRefreshTtl = jwtRefreshTtl; }
    public String getAppUrl() { return appUrl; }
    public void setAppUrl(String appUrl) { this.appUrl = appUrl; }
    public String getCorsOrigins() { return corsOrigins; }
    public void setCorsOrigins(String corsOrigins) { this.corsOrigins = corsOrigins; }
    public boolean isCookieSecure() { return cookieSecure; }
    public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
    public String getCookieSameSite() { return cookieSameSite; }
    public void setCookieSameSite(String cookieSameSite) { this.cookieSameSite = cookieSameSite; }
}
