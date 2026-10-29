package com.developershub.backend.support;

import com.developershub.backend.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

/**
 * Shared setup for API integration tests: boots the real application against
 * the developershub_test database and resets all tables between tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseInitializer.class)
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Password123!";

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RateLimiter rateLimiter;

    @BeforeEach
    void cleanDatabase() {
        rateLimiter.reset();
        jdbc.update("DELETE FROM bookmark");
        jdbc.update("DELETE FROM opportunity_tag");
        jdbc.update("DELETE FROM opportunity");
        jdbc.update("DELETE FROM tag");
        jdbc.update("DELETE FROM users");
    }

    protected HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    protected String extractToken(ResponseEntity<Map<String, Object>> response) {
        return (String) response.getBody().get("token");
    }

    protected String register(String email) {
        ResponseEntity<Map<String, Object>> response = rest.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(Map.of("email", email, "password", PASSWORD), headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Registration failed for " + email + ": " + response.getStatusCode());
        }
        return extractToken(response);
    }

    /** Registers an account, promotes it to ADMIN directly in the database, and logs in. */
    protected String adminToken(String email) {
        register(email);
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);
        return login(email);
    }

    protected String login(String email) {
        ResponseEntity<Map<String, Object>> response = rest.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", PASSWORD), headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Login failed for " + email + ": " + response.getStatusCode());
        }
        return extractToken(response);
    }

    protected Map<String, Object> opportunityBody(String title, String type, String deadline, String... tags) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", title);
        body.put("organization", "Org " + title);
        body.put("type", type);
        body.put("description", "Description of " + title);
        body.put("url", "https://example.com/" + title.replace(' ', '-').toLowerCase());
        if (deadline != null) {
            body.put("deadline", deadline);
        }
        body.put("tags", java.util.Set.of(tags));
        return body;
    }

    protected Long createOpportunity(String adminToken, Map<String, Object> body) {
        ResponseEntity<Map<String, Object>> response = rest.postForEntity(
                "/api/opportunities",
                new HttpEntity<>(body, headers(adminToken)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Opportunity creation failed: " + response.getStatusCode());
        }
        return ((Number) response.getBody().get("id")).longValue();
    }
}
