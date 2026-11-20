package com.developershub.backend.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void nullSecretFallsBackToEphemeralKey() {
        JwtUtil jwtUtil = new JwtUtil(null, 60_000);
        String token = jwtUtil.generateToken("user@test.dev", "USER");

        Optional<Claims> claims = jwtUtil.parseToken(token);

        assertTrue(claims.isPresent());
        assertEquals("user@test.dev", claims.get().getSubject());
    }

    @Test
    void rejectsSecretShorterThan32Bytes() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new JwtUtil("too-short-secret", 60_000));

        assertNotNull(error.getMessage());
        assertTrue(error.getMessage().contains("at least 32 bytes"),
                "expected the minimum key length to be named, but was: " + error.getMessage());
    }

    @Test
    void rejectsTokenWithMutatedSignature() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken("user@test.dev", "USER");
        String[] parts = token.split("\\.");
        // Swap one signature character while keeping the segment length, so the
        // token is rejected by the signature check rather than by the shape check.
        int index = parts[2].length() / 2;
        char replacement = parts[2].charAt(index) == 'A' ? 'B' : 'A';
        parts[2] = parts[2].substring(0, index) + replacement + parts[2].substring(index + 1);

        assertTrue(jwtUtil.parseToken(String.join(".", parts)).isEmpty());
    }

    @Test
    void rejectsTokenWithAppendedTrailingCharacter() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken("user@test.dev", "USER");

        assertTrue(jwtUtil.parseToken(token + "A").isEmpty());
        // The untouched token still parses, so the rejection above is not an artefact.
        assertTrue(jwtUtil.parseToken(token).isPresent());
    }

    @Test
    void rejectsTokenWithWrongSegmentCount() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken("user@test.dev", "USER");

        assertTrue(jwtUtil.parseToken(token + ".extra").isEmpty());
        assertTrue(jwtUtil.parseToken("header.payload").isEmpty());
    }

    @Test
    void rejectsMalformedAndBlankTokens() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);

        assertTrue(jwtUtil.parseToken(null).isEmpty());
        assertTrue(jwtUtil.parseToken("").isEmpty());
        assertTrue(jwtUtil.parseToken("   ").isEmpty());
        assertTrue(jwtUtil.parseToken("not-a-jwt").isEmpty());
        assertTrue(jwtUtil.parseToken("a.b.c").isEmpty());
    }
}
