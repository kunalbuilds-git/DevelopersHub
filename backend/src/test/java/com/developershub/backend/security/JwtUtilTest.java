package com.developershub.backend.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef-0123456789abcdef";

    @Test
    void roundTripsSubjectAndRole() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);

        Optional<Claims> claims = jwtUtil.parseToken(jwtUtil.generateToken("user@test.dev", "ADMIN"));

        assertTrue(claims.isPresent());
        assertEquals("user@test.dev", claims.get().getSubject());
        assertEquals("ADMIN", claims.get().get("role", String.class));
    }

    @Test
    void rejectsExpiredToken() {
        Optional<Claims> claims = new JwtUtil(SECRET, -1_000)
                .parseToken(new JwtUtil(SECRET, -1_000).generateToken("user@test.dev", "USER"));

        assertTrue(claims.isEmpty());
    }

    @Test
    void rejectsTamperedToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken("user@test.dev", "USER");

        assertTrue(jwtUtil.parseToken(token + "x").isEmpty());
        assertTrue(jwtUtil.parseToken(token.substring(0, token.length() - 2)).isEmpty());
    }

    @Test
    void rejectsTokensSignedWithDifferentSecret() {
        String otherSecret = "another-test-secret-0123456789abcdef-0123456789abcdef";
        String token = new JwtUtil(otherSecret, 60_000).generateToken("user@test.dev", "USER");

        assertTrue(new JwtUtil(SECRET, 60_000).parseToken(token).isEmpty());
    }

    @Test
    void blankSecretFallsBackToEphemeralKey() {
        JwtUtil jwtUtil = new JwtUtil("  ", 60_000);
        String token = jwtUtil.generateToken("user@test.dev", "USER");

        assertTrue(jwtUtil.parseToken(token).isPresent());
    }
}
