package com.developershub.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey key;
    private final long expirationMs;
    private final int expectedSignatureBytes;

    public JwtUtil(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-ms:1800000}") long expirationMs) {
        this.expirationMs = expirationMs;
        if (secret == null || secret.isBlank()) {
            byte[] bytes = new byte[64];
            new SecureRandom().nextBytes(bytes);
            this.key = Keys.hmacShaKeyFor(bytes);
            log.warn("jwt.secret is not set - generated an ephemeral random secret. "
                    + "Tokens will be invalidated on every restart. Set JWT_SECRET in production.");
        } else if (secret.getBytes().length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes for HMAC-SHA256");
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes());
        }
        this.expectedSignatureBytes = switch (key.getAlgorithm()) {
            case "HmacSHA384" -> 48;
            case "HmacSHA512" -> 64;
            default -> 32;
        };
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and expiration, returning the claims on success.
     * Returns empty for any malformed, tampered, or expired token.
     */
    public Optional<Claims> parseToken(String token) {
        if (!isStructurallyValid(token)) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * JJWT's base64 decoding is lenient, so a token with characters appended to
     * its signature can still parse. Checking the exact segment count and the
     * decoded signature length rejects that malleability.
     */
    private boolean isStructurallyValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        try {
            byte[] header = Base64.getUrlDecoder().decode(parts[0]);
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
            return header.length > 0 && payload.length > 0 && signature.length == expectedSignatureBytes;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
