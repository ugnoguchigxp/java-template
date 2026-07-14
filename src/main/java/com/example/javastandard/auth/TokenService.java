package com.example.javastandard.auth;

import com.example.javastandard.config.AppProperties;
import com.example.javastandard.db.model.RefreshTokenRecord;
import com.example.javastandard.db.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private static final String ISSUER = "java25-postgresql-template";
    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([smhd])$");
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RefreshTokenRepository refreshTokens;
    private final AppProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            RefreshTokenRepository refreshTokens,
            AppProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
    }

    public String issueAccessToken(AuthUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(parseDuration(properties.getJwtAccessTtl()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .claim("type", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String issueRefreshToken(AuthUser user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long now = System.currentTimeMillis();
        RefreshTokenRecord record = new RefreshTokenRecord();
        record.setId(UUID.randomUUID().toString());
        record.setTokenHash(hash(token));
        record.setUserId(user.getId());
        record.setCreatedAt(now);
        long refreshMillis = Math.multiplyExact(parseDuration(properties.getJwtRefreshTtl()), 1000L);
        record.setExpiresAt(Math.addExact(now, refreshMillis));
        refreshTokens.insert(record);
        return token;
    }

    public AuthPrincipal parseAccessToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String role = jwt.getClaimAsString("role");
            if (!ISSUER.equals(jwt.getClaimAsString("iss"))
                    || !"access".equals(jwt.getClaimAsString("type"))
                    || jwt.getSubject() == null
                    || jwt.getClaimAsString("email") == null
                    || !("admin".equals(role) || "member".equals(role))) {
                throw new AuthException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "AUTH_UNAUTHORIZED", "Unauthorized");
            }
            return new AuthPrincipal(jwt.getSubject(), jwt.getClaimAsString("email"),
                    role);
        } catch (AuthException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "AUTH_UNAUTHORIZED", "Unauthorized");
        }
    }

    public String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash token.", exception);
        }
    }

    public long parseDuration(String value) {
        Matcher matcher = DURATION_PATTERN.matcher(value == null ? "" : value);
        if (!matcher.matches()) {
            throw new IllegalStateException("Invalid duration.");
        }
        try {
            long amount = Long.parseLong(matcher.group(1));
            if (amount <= 0) throw new IllegalStateException("Invalid duration.");
            switch (matcher.group(2).charAt(0)) {
                case 's': return amount;
                case 'm': return Math.multiplyExact(amount, 60L);
                case 'h': return Math.multiplyExact(amount, 60L * 60L);
                case 'd': return Math.multiplyExact(amount, 60L * 60L * 24L);
                default: throw new IllegalStateException("Invalid duration.");
            }
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalStateException("Invalid duration.", exception);
        }
    }
}
