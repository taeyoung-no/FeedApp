package com.feedapp.server.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String createAccessToken(String username) {
        final var now = new Date();
        final var expiry = new Date(now.getTime() + accessExpirationMs);
        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String username) {
        return createRefreshToken(username, UUID.randomUUID().toString());
    }

    public String createRefreshToken(String username, String sid) {
        final var now = new Date();
        final var expiry = new Date(now.getTime() + refreshExpirationMs);
        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim("type", "refresh")
                .claim("sid", sid)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String getType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public String getJti(String token) {
        return parseClaims(token).getId();
    }

    public String getSid(String token) {
        return parseClaims(token).get("sid", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
