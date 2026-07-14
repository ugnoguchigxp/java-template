package com.example.javastandard.auth;

import com.example.javastandard.db.model.RefreshTokenRecord;
import com.example.javastandard.db.model.UserRecord;
import com.example.javastandard.db.repository.RefreshTokenRepository;
import com.example.javastandard.db.repository.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final Object refreshLock = new Object();
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public UserRecord findUserById(String id) {
        return users.findById(id);
    }

    public UserRecord findUserByEmail(String email) {
        return users.findByNormalizedEmail(normalizeEmail(email));
    }

    @Transactional
    public AuthResult login(String email, String password) {
        UserRecord user = users.findByNormalizedEmail(normalizeEmail(email));
        if (user == null || !user.isActive() || password == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Invalid email or password.");
        }
        long now = System.currentTimeMillis();
        users.updateLastLogin(user.getId(), now);
        user.setUpdatedAt(now);
        user.setLastLoginAt(now);
        return issue(new AuthUser(user));
    }

    @Transactional
    public AuthResult refresh(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Invalid refresh token.");
        }
        synchronized (refreshLock) {
            String tokenHash = tokenService.hash(token);
            RefreshTokenRecord stored = refreshTokens.findByHash(tokenHash);
            if (stored == null || stored.getExpiresAt() <= System.currentTimeMillis()) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                        "Invalid refresh token.");
            }
            int deleted = refreshTokens.consumeIfUnexpired(
                    tokenHash, System.currentTimeMillis());
            if (deleted != 1) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                        "Invalid refresh token.");
            }
            UserRecord user = users.findById(stored.getUserId());
            if (user == null || !user.isActive()) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                        "User account is inactive or deleted.");
            }
            return issue(new AuthUser(user));
        }
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.trim().isEmpty()) {
            refreshTokens.deleteByHash(tokenService.hash(token));
        }
    }

    @Transactional
    public AuthUser createAdmin(String email, String displayName, String password) {
        String normalized = normalizeEmail(email);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                || password == null || password.length() < 12) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "AUTH_INVALID_INPUT",
                    "Email and a password of at least 12 characters are required.");
        }
        if (users.findByNormalizedEmail(normalized) != null) {
            throw new AuthException(HttpStatus.CONFLICT, "AUTH_EMAIL_EXISTS", "Email already in use.");
        }
        long now = System.currentTimeMillis();
        UserRecord user = new UserRecord();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(normalized);
        user.setNormalizedEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName == null || displayName.trim().isEmpty() ? "Admin User" : displayName.trim());
        user.setRole("admin");
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        users.insert(user);
        return new AuthUser(user);
    }

    private AuthResult issue(AuthUser user) {
        return new AuthResult(tokenService.issueAccessToken(user),
                tokenService.issueRefreshToken(user), user);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
