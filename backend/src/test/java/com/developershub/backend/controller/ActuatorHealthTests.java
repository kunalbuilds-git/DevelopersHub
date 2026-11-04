package com.developershub.backend.controller;

import com.developershub.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #32 - the health endpoint must be reachable without a token, report
 * UP while the database is reachable, and must not leak details or expose any
 * other actuator endpoint.
 */
class ActuatorHealthTests extends IntegrationTestBase {

    @SuppressWarnings("unchecked")
    @Test
    void healthIsPublicAndReportsUp() {
        ResponseEntity<Map> response = rest.exchange(
                "/actuator/health",
                HttpMethod.GET,
                new HttpEntity<>(headers(null)),
                Map.class);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().get("status"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void healthDoesNotExposeDetails() {
        ResponseEntity<Map> response = rest.exchange(
                "/actuator/health",
                HttpMethod.GET,
                new HttpEntity<>(headers(null)),
                Map.class);

        assertFalse(response.getBody().containsKey("components"), "health details must not be public");
        assertFalse(response.getBody().containsKey("details"), "health details must not be public");
    }

    @Test
    void noOtherActuatorEndpointsAreExposed() {
        for (String path : new String[] {"/actuator", "/actuator/info", "/actuator/env", "/actuator/metrics"}) {
            ResponseEntity<String> response = rest.exchange(
                    path,
                    HttpMethod.GET,
                    new HttpEntity<>(headers(null)),
                    String.class);
            // Anonymous callers are rejected before the (missing) handler runs,
            // and an authenticated caller gets the plain 404 of a non-exposed endpoint.
            assertFalse(response.getStatusCode().is2xxSuccessful(), path + " must not be exposed");
        }
    }

    @Test
    void nonExposedActuatorEndpointsStayUnreachableForAuthenticatedCallers() {
        String token = register("health-user@test.dev");

        for (String path : new String[] {"/actuator/info", "/actuator/env", "/actuator/metrics"}) {
            ResponseEntity<String> response = rest.exchange(
                    path,
                    HttpMethod.GET,
                    new HttpEntity<>(headers(token)),
                    String.class);
            assertFalse(response.getStatusCode().is2xxSuccessful(),
                    path + " must not be reachable, even with a valid token");
        }
    }

    @Test
    void livenessAndReadinessProbesAreAvailable() {
        for (String path : new String[] {"/actuator/health/liveness", "/actuator/health/readiness"}) {
            ResponseEntity<String> response = rest.exchange(
                    path,
                    HttpMethod.GET,
                    new HttpEntity<>(headers(null)),
                    String.class);
            assertTrue(response.getStatusCode().is2xxSuccessful(), path + " should be reachable");
        }
    }
}
