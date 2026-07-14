package com.example.javastandard.auth;

import com.example.javastandard.config.AppProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthCookieService {
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String CSRF_TOKEN = "XSRF-TOKEN";

    private final AppProperties properties;
    private final TokenService tokens;

    public AuthCookieService(AppProperties properties, TokenService tokens) {
        this.properties = properties;
        this.tokens = tokens;
    }

    public void setAuthCookies(HttpServletResponse response, AuthResult result) {
        response.addHeader("Set-Cookie", cookie(ACCESS_TOKEN, result.getAccessToken(), "/", tokens.parseDuration(properties.getJwtAccessTtl()), true));
        response.addHeader("Set-Cookie", cookie(REFRESH_TOKEN, result.getRefreshToken(), "/api/auth", tokens.parseDuration(properties.getJwtRefreshTtl()), true));
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookie(ACCESS_TOKEN, "", "/", 0, true));
        response.addHeader("Set-Cookie", cookie(REFRESH_TOKEN, "", "/api/auth", 0, true));
    }

    public String csrfCookie(String token) {
        ResponseCookie cookie = ResponseCookie.from(CSRF_TOKEN, token)
                .httpOnly(false)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path("/")
                .maxAge(Duration.ofHours(8))
                .build();
        return cookie.toString();
    }

    private String cookie(String name, String value, String path, long maxAge, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path(path)
                .maxAge(Duration.ofSeconds(maxAge))
                .build()
                .toString();
    }
}
