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

class BookmarkApiTests extends IntegrationTestBase {

    private String alice;
    private String bob;

    private String alice() {
        if (alice == null) alice = register("alice@test.dev");
        return alice;
    }

    private String bob() {
        if (bob == null) bob = register("bob@test.dev");
        return bob;
    }

    private ResponseEntity<Void> bookmark(String token, long opportunityId, HttpMethod method) {
        return rest.exchange(
                "/api/bookmarks/" + opportunityId,
                method,
                new HttpEntity<>(headers(token)),
                Void.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listBookmarks(String token) {
        ResponseEntity<List> response = rest.exchange(
                "/api/bookmarks",
                HttpMethod.GET,
                new HttpEntity<>(headers(token)),
                List.class);
        assertEquals(200, response.getStatusCode().value());
        return (List<Map<String, Object>>) response.getBody();
    }

    @Test
    void anonymousCannotBookmark() {
        Long id = createOpportunity(adminToken("admin@test.dev"),
                opportunityBody("Target", "HACKATHON", null));

        assertEquals(403, bookmark(null, id, HttpMethod.POST).getStatusCode().value());
    }

    @Test
    void addListAndRemoveBookmark() {
        String token = alice();
        Long id = createOpportunity(adminToken("admin@test.dev"),
                opportunityBody("Bookmarkable", "HACKATHON", "2026-12-01T10:00:00", "Java"));

        assertEquals(201, bookmark(token, id, HttpMethod.POST).getStatusCode().value());

        List<Map<String, Object>> bookmarks = listBookmarks(token);
        assertEquals(1, bookmarks.size());
        assertEquals(id, ((Number) bookmarks.get(0).get("opportunityId")).longValue());
        assertEquals("Bookmarkable", bookmarks.get(0).get("title"));

        assertEquals(204, bookmark(token, id, HttpMethod.DELETE).getStatusCode().value());
        assertTrue(listBookmarks(token).isEmpty());
    }

    @Test
    void duplicateBookmarkIsConflict() {
        String token = alice();
        Long id = createOpportunity(adminToken("admin@test.dev"),
                opportunityBody("Once Only", "PROGRAM", null));

        assertEquals(201, bookmark(token, id, HttpMethod.POST).getStatusCode().value());
        assertEquals(409, bookmark(token, id, HttpMethod.POST).getStatusCode().value());
    }

    @Test
    void removingMissingBookmarkIsNotFound() {
        assertEquals(404, bookmark(alice(), 99999, HttpMethod.DELETE).getStatusCode().value());
    }

    @Test
    void bookmarksAreSortedByDeadlineWithNullsLast() {
        String token = alice();
        String admin = adminToken("admin@test.dev");
        Long december = createOpportunity(admin,
                opportunityBody("December Event", "HACKATHON", "2026-12-20T10:00:00"));
        Long june = createOpportunity(admin,
                opportunityBody("June Event", "INTERNSHIP", "2026-06-15T10:00:00"));
        Long noDeadline = createOpportunity(admin,
                opportunityBody("No Deadline", "PROGRAM", null));

        bookmark(token, december, HttpMethod.POST);
        bookmark(token, noDeadline, HttpMethod.POST);
        bookmark(token, june, HttpMethod.POST);

        List<Map<String, Object>> bookmarks = listBookmarks(token);
        assertEquals(3, bookmarks.size());
        assertEquals(june, ((Number) bookmarks.get(0).get("opportunityId")).longValue());
        assertEquals(december, ((Number) bookmarks.get(1).get("opportunityId")).longValue());
        assertEquals(noDeadline, ((Number) bookmarks.get(2).get("opportunityId")).longValue());
    }

    @Test
    void bookmarksArePrivateToTheirOwner() {
        Long id = createOpportunity(adminToken("admin@test.dev"),
                opportunityBody("Private", "PROGRAM", null));

        bookmark(alice(), id, HttpMethod.POST);

        assertTrue(listBookmarks(bob()).isEmpty());
    }

    @Test
    void deletingOpportunityRemovesItsBookmarks() {
        String token = alice();
        Long id = createOpportunity(adminToken("admin@test.dev"),
                opportunityBody("Cascading", "PROGRAM", null));

        bookmark(token, id, HttpMethod.POST);
        rest.exchange("/api/opportunities/" + id, HttpMethod.DELETE,
                new HttpEntity<>(headers(adminToken("admin2@test.dev"))), Void.class);

        assertTrue(listBookmarks(token).isEmpty());
    }
}
