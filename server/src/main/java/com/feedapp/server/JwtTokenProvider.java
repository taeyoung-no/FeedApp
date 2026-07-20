package com.feedapp.server;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

	private final SecretKey key;
	private final long expirationMs;

	public JwtTokenProvider(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.expiration-ms}") long expirationMs
	) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String createToken(String username) {
		final var now = new Date();
		final var expiry = new Date(now.getTime() + expirationMs);
		return Jwts.builder()
				.subject(username)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}
}
