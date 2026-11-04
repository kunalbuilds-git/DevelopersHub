package com.developershub.backend.controller;

import com.developershub.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #35 - a user can list their own submissions, and only their own.
 */
class MySubmissionsTests extends IntegrationTestBase {

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> submit(String token, String title) {
        return rest.exchange(
                "/api/opportunities/submissions",
                HttpMethod.POST,
                new HttpEntity<>(opportunityBody(title, "EVENT", null, "community"), headers(token)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> mine(String token, String query) {
        return rest.exchange(
                "/api/opportunities/submissions/mine" + (query == null ? "" : query),
                HttpMethod.GET,
                new HttpEntity<>(headers(token)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @Test
    void userSeesTheirOwnSubmissionsWithStatus() {
        String alice = register("alice@test.dev");
        ResponseEntity<Map<String, Object>> created = submit(alice, "Alice's Meetup");
        assertEquals(201, created.getStatusCode().value());
        assertEquals("PENDING", created.getBody().get("status"));

        ResponseEntity<Map<String, Object>> mineResponse = mine(alice, null);

        assertEquals(200, mineResponse.getStatusCode().value());
        List<Map<String, Object>> content = (List<Map<String, Object>>) mineResponse.getBody().get("content");
        assertEquals(1, content.size());
        assertEquals("Alice's Meetup", content.get(0).get("title"));
        assertEquals("PENDING", content.get(0).get("status"));
    }

    @Test
    void userNeverSeesAnotherUsersSubmissions() {
        String alice = register("alice@test.dev");
        String bob = register("bob@test.dev");
        submit(alice, "Alice's Meetup");

        ResponseEntity<Map<String, Object>> bobView = mine(bob, null);

        List<Map<String, Object>> content = (List<Map<String, Object>>) bobView.getBody().get("content");
        assertTrue(content.isEmpty(), "bob must not see alice's submissions");
        assertEquals(0, ((Number) bobView.getBody().get("totalElements")).longValue());
    }

    @Test
    void unauthenticatedRequestsGet401() {
        assertEquals(401, mine(null, null).getStatusCode().value());
    }

    @Test
    void statusFilterNarrowsMySubmissions() {
        String alice = register("alice@test.dev");
        String admin = adminToken("admin@test.dev");
        ResponseEntity<Map<String, Object>> created = submit(alice, "Alice's Meetup");
        long id = ((Number) created.getBody().get("id")).longValue();

        // Approve it as an admin, then check the status filter from the user's side.
        rest.exchange("/api/opportunities/submissions/" + id + "/approve", HttpMethod.POST,
                new HttpEntity<>(headers(admin)), Map.class);

        List<Map<String, Object>> pending =
                (List<Map<String, Object>>) mine(alice, "?status=PENDING").getBody().get("content");
        List<Map<String, Object>> approved =
                (List<Map<String, Object>>) mine(alice, "?status=APPROVED").getBody().get("content");

        assertTrue(pending.isEmpty(), "nothing should be pending after approval");
        assertEquals(1, approved.size());
    }

    @Test
    void routeDoesNotClashWithTheAdminQueueOrReviewActions() {
        String alice = register("alice@test.dev");
        String admin = adminToken("admin@test.dev");
        submit(alice, "Alice's Meetup");

        // The admin queue still lists the pending submission...
        ResponseEntity<Map<String, Object>> queue = rest.exchange(
                "/api/opportunities/submissions",
                HttpMethod.GET,
                new HttpEntity<>(headers(admin)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertEquals(200, queue.getStatusCode().value());
        assertEquals(1, ((Number) queue.getBody().get("totalElements")).longValue());

        // ...and the review action still works on it.
        long id = ((Number) ((List<Map<String, Object>>) queue.getBody().get("content")).get(0).get("id")).longValue();
        ResponseEntity<Map<String, Object>> approve = rest.exchange(
                "/api/opportunities/submissions/" + id + "/approve",
                HttpMethod.POST,
                new HttpEntity<>(headers(admin)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertEquals(200, approve.getStatusCode().value());
    }
}
