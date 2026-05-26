package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String createAccessToken(Long userId, String username) {
        return createToken(userId, username, "access", jwtProperties.getAccessTokenExpireHours());
    }

    public String createRefreshToken(Long userId, String username) {
        return createToken(userId, username, "refresh", jwtProperties.getRefreshTokenExpireHours());
    }

    public Optional<Long> parseRefreshUserId(String token) {
        return parseToken(token, "refresh");
    }

    private String createToken(Long userId, String username, String type, int expireHours) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(expireHours, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(signingKey())
                .compact();
    }

    public Optional<Long> parseUserId(String token) {
        return parseToken(token, "access");
    }

    private Optional<Long> parseToken(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            String type = claims.get("type", String.class);
            if (type != null && !expectedType.equals(type)) {
                return Optional.empty();
            }
            if (type == null && !"access".equals(expectedType)) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private SecretKey signingKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
