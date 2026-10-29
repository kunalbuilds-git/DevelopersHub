package com.developershub.backend.controller;

import com.developershub.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthRefreshTests extends IntegrationTestBase {

    @SuppressWarnings("unchecked")
    private Map<String, Object> registerBody(String email) {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(Map.of("email", email, "password", PASSWORD), headers(null)),
                Map.class);
        assertEquals(201, response.getStatusCode().value());
        return (Map<String, Object>) response.getBody();
    }

    private ResponseEntity<Map> refresh(String refreshToken) {
        return rest.postForEntity(
                "/api/auth/refresh",
                new HttpEntity<>(Map.of("refreshToken", refreshToken), headers(null)),
                Map.class);
    }

    @Test
    void registerReturnsBothTokens() {
        Map<String, Object> body = registerBody("alice@test.dev");

        assertNotNull(body.get("token"));
        assertNotNull(body.get("refreshToken"));
    }

    @Test
    void refreshRotatesTokensAndRetiresTheOldRefreshToken() {
        String oldRefresh = (String) registerBody("alice@test.dev").get("refreshToken");

        ResponseEntity<Map> response = refresh(oldRefresh);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody().get("token"));
        String newRefresh = (String) response.getBody().get("refreshToken");

        // Single-use: the retired token can never be replayed.
        assertEquals(401, refresh(oldRefresh).getStatusCode().value());
        assertEquals(200, refresh(newRefresh).getStatusCode().value());
    }

    @Test
    void refreshWithUnknownTokenIsUnauthorized() {
        assertEquals(401, refresh("not-a-real-refresh-token").getStatusCode().value());
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        String refreshToken = (String) registerBody("alice@test.dev").get("refreshToken");

        ResponseEntity<Void> logout = rest.postForEntity(
                "/api/auth/logout",
                new HttpEntity<>(Map.of("refreshToken", refreshToken), headers(null)),
                Void.class);

        assertEquals(204, logout.getStatusCode().value());
        assertEquals(401, refresh(refreshToken).getStatusCode().value());
    }

    @Test
    void refreshedAccessTokenIsAcceptedByProtectedEndpoints() {
        Map<String, Object> refreshed = (Map<String, Object>)
                refresh((String) registerBody("alice@test.dev").get("refreshToken")).getBody();
        String accessToken = (String) refreshed.get("token");

        ResponseEntity<List> bookmarks = rest.exchange(
                "/api/bookmarks",
                HttpMethod.GET,
                new HttpEntity<>(headers(accessToken)),
                List.class);

        assertEquals(200, bookmarks.getStatusCode().value());
    }
}
