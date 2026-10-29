package com.developershub.backend.controller;

import com.developershub.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitApiTests extends IntegrationTestBase {

    private ResponseEntity<?> login(String email, String password) {
        return rest.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password), headers(null)),
                Map.class);
    }

    private ResponseEntity<?> registerFlood(String email) {
        return rest.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(Map.of("email", email, "password", PASSWORD), headers(null)),
                Map.class);
    }

    @Test
    void loginIsRateLimitedAfterTheConfiguredAttemptBudget() {
        // Test profile allows 5 login attempts per window.
        for (int i = 0; i < 5; i++) {
            assertEquals(401, login("ghost@test.dev", "WrongPassword1!").getStatusCode().value());
        }
        assertEquals(429, login("ghost@test.dev", "WrongPassword1!").getStatusCode().value());
    }

    @Test
    void registerIsRateLimitedAfterTheConfiguredAttemptBudget() {
        // Test profile allows 10 registration attempts per window; responses
        // alternate between 201 (new) and 409 (duplicate) but all count.
        for (int i = 0; i < 10; i++) {
            registerFlood("flood@test.dev");
        }
        assertEquals(429, registerFlood("flood@test.dev").getStatusCode().value());
    }

    @Test
    void loginBudgetIsNotSharedWithRegister() {
        for (int i = 0; i < 5; i++) {
            assertEquals(401, login("ghost@test.dev", "WrongPassword1!").getStatusCode().value());
        }
        assertEquals(429, login("ghost@test.dev", "WrongPassword1!").getStatusCode().value());

        // The register bucket is separate, so registration still succeeds.
        assertEquals(201, registerFlood("still-allowed@test.dev").getStatusCode().value());
    }
}
