package com.developershub.backend.controller;

import com.developershub.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Runs in its own context because the setup token is read from configuration.
 */
@TestPropertySource(properties = "app.admin.setup-token=bootstrap-test-token")
class BootstrapApiTests extends IntegrationTestBase {

    private static final String SETUP_TOKEN = "bootstrap-test-token";

    private ResponseEntity<Map<String, Object>> bootstrap(String email, String setupToken) {
        return rest.postForEntity(
                "/api/auth/admin/bootstrap",
                new HttpEntity<>(Map.of("email", email, "setupToken", setupToken), headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @Test
    void firstAccountCanClaimTheAdminRole() {
        register("founder@test.dev");

        ResponseEntity<Map<String, Object>> response = bootstrap("founder@test.dev", SETUP_TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody().get("token"));

        String adminToken = (String) response.getBody().get("token");
        ResponseEntity<Map<String, Object>> created = rest.exchange(
                "/api/opportunities",
                HttpMethod.POST,
                new HttpEntity<>(opportunityBody("Made By Bootstrap", "PROGRAM", null), headers(adminToken)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertEquals(201, created.getStatusCode().value());
    }

    @Test
    void wrongSetupTokenIsForbidden() {
        register("someone@test.dev");

        assertEquals(403, bootstrap("someone@test.dev", "totally-wrong-token").getStatusCode().value());
    }

    @Test
    void bootstrapOnlyWorksWhileNoAdminExists() {
        register("first@test.dev");
        register("second@test.dev");

        assertEquals(200, bootstrap("first@test.dev", SETUP_TOKEN).getStatusCode().value());
        assertEquals(409, bootstrap("second@test.dev", SETUP_TOKEN).getStatusCode().value());
    }

    @Test
    void bootstrapWithoutAnExistingAccountIsNotFound() {
        assertEquals(404, bootstrap("ghost@test.dev", SETUP_TOKEN).getStatusCode().value());
    }
}
