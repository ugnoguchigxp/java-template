package com.example.javastandard.web;

import com.example.javastandard.auth.AuthCookieService;
import com.example.javastandard.auth.AuthException;
import com.example.javastandard.auth.AuthPrincipal;
import com.example.javastandard.auth.AuthResult;
import com.example.javastandard.auth.AuthService;
import com.example.javastandard.auth.AuthUser;
import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService cookies;

    public AuthController(AuthService authService, AuthCookieService cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @GetMapping("/csrf")
    public java.util.Map<String, String> csrf(HttpServletResponse response) {
        String token = UUID.randomUUID().toString();
        response.addHeader("Set-Cookie", cookies.csrfCookie(token));
        java.util.Map<String, String> result = new java.util.LinkedHashMap<String, String>();
        result.put("token", token);
        return result;
    }

    @PostMapping("/auth/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResult result = authService.login(request.getEmail(), request.getPassword());
        cookies.setAuthCookies(response, result);
        return new AuthResponse(new SessionUser(result.getUser()));
    }

    @PostMapping("/auth/refresh")
    public AuthResponse refresh(
            @CookieValue(value = AuthCookieService.REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "Unauthorized");
        }
        AuthResult result = authService.refresh(refreshToken);
        cookies.setAuthCookies(response, result);
        return new AuthResponse(new SessionUser(result.getUser()));
    }

    @PostMapping("/auth/logout")
    public LogoutResponse logout(
            @CookieValue(value = AuthCookieService.REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        cookies.clearAuthCookies(response);
        return new LogoutResponse();
    }

    @GetMapping("/auth/me")
    public AuthResponse me(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        com.example.javastandard.db.model.UserRecord record =
                authService.findUserById(principal.getUserId());
        if (record == null || !record.isActive()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "Unauthorized");
        }
        return new AuthResponse(new SessionUser(new AuthUser(record)));
    }
}
