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

class SubmissionApiTests extends IntegrationTestBase {

    private String user;
    private String admin;

    private String user() {
        if (user == null) user = register("submitter@test.dev");
        return user;
    }

    private String admin() {
        if (admin == null) admin = adminToken("admin@test.dev");
        return admin;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> submit(String token, String title) {
        return rest.exchange(
                "/api/opportunities/submissions",
                HttpMethod.POST,
                new HttpEntity<>(opportunityBody(title, "HACKATHON", "2027-06-01T10:00:00", "java"), headers(token)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> review(String token, long id, String action) {
        return rest.exchange(
                "/api/opportunities/submissions/" + id + "/" + action,
                HttpMethod.POST,
                new HttpEntity<>(headers(token)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> publicList(String query) {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/api/opportunities" + (query == null ? "" : query),
                HttpMethod.GET,
                new HttpEntity<>(headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        return (List<Map<String, Object>>) response.getBody().get("content");
    }

    @Test
    void userSubmissionStaysHiddenUntilAnAdminApprovesIt() {
        ResponseEntity<Map<String, Object>> created = submit(user(), "Community Hackathon");
        assertEquals(201, created.getStatusCode().value());
        assertEquals("PENDING", created.getBody().get("status"));
        long id = ((Number) created.getBody().get("id")).longValue();

        // Hidden from the public listing and detail endpoint...
        assertEquals(0, publicList(null).size());
        assertEquals(404, rest.exchange("/api/opportunities/" + id, HttpMethod.GET,
                new HttpEntity<>(headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class).getStatusCode().value());

        // ...and visible to the admin as a pending submission.
        ResponseEntity<Map<String, Object>> queue = rest.exchange(
                "/api/opportunities/submissions",
                HttpMethod.GET,
                new HttpEntity<>(headers(admin())),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        List<Map<String, Object>> pending = (List<Map<String, Object>>) queue.getBody().get("content");
        assertEquals(1, pending.size());
        assertEquals("PENDING", pending.get(0).get("status"));

        // Approving makes it public.
        assertEquals(200, review(admin(), id, "approve").getStatusCode().value());
        assertEquals(1, publicList(null).size());
        assertEquals(200, rest.exchange("/api/opportunities/" + id, HttpMethod.GET,
                new HttpEntity<>(headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class).getStatusCode().value());
    }

    @Test
    void approvingTwiceIsConflict() {
        long id = ((Number) submit(user(), "Once Only").getBody().get("id")).longValue();

        assertEquals(200, review(admin(), id, "approve").getStatusCode().value());
        assertEquals(409, review(admin(), id, "approve").getStatusCode().value());
    }

    @Test
    void rejectedSubmissionsStayHidden() {
        long id = ((Number) submit(user(), "Not Good Enough").getBody().get("id")).longValue();

        ResponseEntity<Map<String, Object>> rejected = review(admin(), id, "reject");
        assertEquals(200, rejected.getStatusCode().value());
        assertEquals("REJECTED", rejected.getBody().get("status"));

        assertEquals(0, publicList(null).size());
        ResponseEntity<Map<String, Object>> queue = rest.exchange(
                "/api/opportunities/submissions?status=REJECTED",
                HttpMethod.GET,
                new HttpEntity<>(headers(admin())),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
        List<Map<String, Object>> rejectedList = (List<Map<String, Object>>) queue.getBody().get("content");
        assertEquals(1, rejectedList.size());
    }

    @Test
    void nonAdminsCannotReviewSubmissions() {
        long id = ((Number) submit(user(), "Locked Review").getBody().get("id")).longValue();

        assertEquals(403, review(user(), id, "approve").getStatusCode().value());
        assertEquals(403, rest.exchange("/api/opportunities/submissions", HttpMethod.GET,
                new HttpEntity<>(headers(user())),
                (Class<Map<String, Object>>) (Class<?>) Map.class).getStatusCode().value());
    }

    @Test
    void pendingSubmissionsCannotBeBookmarked() {
        long id = ((Number) submit(user(), "Unbookmarkable").getBody().get("id")).longValue();

        ResponseEntity<Void> bookmark = rest.exchange(
                "/api/bookmarks/" + id,
                HttpMethod.POST,
                new HttpEntity<>(headers(user())),
                Void.class);

        assertEquals(404, bookmark.getStatusCode().value());
    }

    @Test
    void responseExposesDaysUntilDeadline() {
        ResponseEntity<Map<String, Object>> created = submit(user(), "Dated");

        assertEquals(201, created.getStatusCode().value());
        assertNotNull(created.getBody().get("daysUntilDeadline"));
    }
}
