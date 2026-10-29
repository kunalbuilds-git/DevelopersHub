package com.developershub.backend.controller;

import com.developershub.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthApiTests extends IntegrationTestBase {

    private ResponseEntity<Map<String, Object>> register(String email, String password) {
        return rest.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(Map.of("email", email, "password", password), headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    private ResponseEntity<Map<String, Object>> login(String email, String password) {
        return rest.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password), headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @Test
    void registerReturnsCreatedWithToken() {
        ResponseEntity<Map<String, Object>> response = register("alice@test.dev", PASSWORD);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(extractToken(response));
    }

    @Test
    void duplicateRegistrationIsConflict() {
        register("alice@test.dev", PASSWORD);

        assertEquals(409, register("alice@test.dev", PASSWORD).getStatusCode().value());
    }

    @Test
    void invalidEmailIsRejected() {
        assertEquals(400, register("not-an-email", PASSWORD).getStatusCode().value());
    }

    @Test
    void blankEmailIsRejected() {
        assertEquals(400, register("", PASSWORD).getStatusCode().value());
    }

    @Test
    void shortPasswordIsRejected() {
        assertEquals(400, register("alice@test.dev", "short").getStatusCode().value());
    }

    @Test
    void loginReturnsToken() {
        register("alice@test.dev", PASSWORD);

        ResponseEntity<Map<String, Object>> response = login("alice@test.dev", PASSWORD);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(extractToken(response));
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {
        register("alice@test.dev", PASSWORD);

        assertEquals(401, login("alice@test.dev", "WrongPassword1!").getStatusCode().value());
    }

    @Test
    void loginWithUnknownEmailIsUnauthorized() {
        assertEquals(401, login("ghost@test.dev", PASSWORD).getStatusCode().value());
    }

    @Test
    void bootstrapWithoutSetupTokenIsUnavailable() {
        register("alice@test.dev", PASSWORD);

        ResponseEntity<Map<String, Object>> response = rest.postForEntity(
                "/api/auth/admin/bootstrap",
                new HttpEntity<>(Map.of("email", "alice@test.dev", "setupToken", "anything"), headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertEquals(503, response.getStatusCode().value());
    }

    @Test
    void roleSuppliedByClientIsIgnored() {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "email", "escalating@test.dev",
                        "password", PASSWORD,
                        "role", "ADMIN"), headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertEquals(201, response.getStatusCode().value());
        String token = extractToken(response);
        // The token must not carry ADMIN: creating opportunities must be forbidden.
        ResponseEntity<Map<String, Object>> create = rest.exchange(
                "/api/opportunities",
                HttpMethod.POST,
                new HttpEntity<>(opportunityBody("Escalation Probe", "HACKATHON", null), headers(token)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertTrue(create.getStatusCode().value() == 403,
                "expected 403 for a freshly registered user, got " + create.getStatusCode());
    }
}
