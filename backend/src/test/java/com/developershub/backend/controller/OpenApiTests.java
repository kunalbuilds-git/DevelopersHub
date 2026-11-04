package com.developershub.backend.controller;

import com.developershub.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #33 - the OpenAPI description must be public, cover all three
 * controller groups with schemas, and advertise the bearer scheme so the
 * Swagger UI "Authorize" button works.
 */
class OpenApiTests extends IntegrationTestBase {

    @SuppressWarnings("unchecked")
    private Map<String, Object> apiDocs() {
        ResponseEntity<Map> response = rest.exchange(
                "/v3/api-docs",
                HttpMethod.GET,
                new HttpEntity<>(headers(null)),
                Map.class);
        assertEquals(200, response.getStatusCode().value());
        return response.getBody();
    }

    @Test
    void apiDescriptionIsPublic() {
        assertTrue(apiDocs().containsKey("paths"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void allThreeControllerGroupsAreDocumented() {
        Map<String, Object> paths = (Map<String, Object>) apiDocs().get("paths");

        assertTrue(paths.keySet().stream().anyMatch(p -> p.startsWith("/api/opportunities")),
                "opportunities paths missing");
        assertTrue(paths.keySet().stream().anyMatch(p -> p.startsWith("/api/auth")),
                "auth paths missing");
        assertTrue(paths.keySet().stream().anyMatch(p -> p.startsWith("/api/bookmarks")),
                "bookmark paths missing");
    }

    @SuppressWarnings("unchecked")
    @Test
    void requestAndResponseSchemasArePresent() {
        Map<String, Object> components = (Map<String, Object>) apiDocs().get("components");
        assertTrue(components.containsKey("schemas"), "schemas missing");

        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        assertTrue(schemas.keySet().stream().anyMatch(s -> s.contains("Opportunity")),
                "opportunity schemas missing");
        assertTrue(schemas.keySet().stream().anyMatch(s -> s.contains("Auth")),
                "auth schemas missing");
    }

    @SuppressWarnings("unchecked")
    @Test
    void bearerSchemeIsAdvertisedForTheAuthorizeButton() {
        Map<String, Object> components = (Map<String, Object>) apiDocs().get("components");
        Map<String, Object> schemes = (Map<String, Object>) components.get("securitySchemes");

        assertTrue(schemes.containsKey("bearerAuth"), "bearerAuth security scheme missing");
        Map<String, Object> bearer = (Map<String, Object>) schemes.get("bearerAuth");
        assertEquals("http", bearer.get("type"));
        assertEquals("bearer", bearer.get("scheme"));
    }
}
