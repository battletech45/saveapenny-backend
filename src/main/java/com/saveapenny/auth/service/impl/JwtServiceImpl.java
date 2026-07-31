package com.saveapenny.auth.service.impl;

import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.TimeService;
import com.saveapenny.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900L;
    // HS512 requires a >=512-bit (64-byte) key; also the documented minimum in docs/security.md.
    private static final int MIN_SECRET_BYTES = 64;

    private final SecretKey signingKey;
    private final TimeService timeService;

    public JwtServiceImpl(@Value("${security.jwt.secret}") String jwtSecret, TimeService timeService) {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret must be at least " + MIN_SECRET_BYTES
                            + " characters (HS512 requires a 512-bit key); got " + secretBytes.length
                            + ". Generate one with: openssl rand -base64 64");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.timeService = timeService;
    }

    @Override
    public String generateAccessToken(User user) {
        Instant now = timeService.now();
        Instant expiry = now.plus(ACCESS_TOKEN_EXPIRY_SECONDS, ChronoUnit.SECONDS);

        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public boolean isAccessTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    @Override
    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    @Override
    public long getAccessTokenExpirySeconds() {
        return ACCESS_TOKEN_EXPIRY_SECONDS;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
