package com.developershub.backend.controller;

import com.developershub.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpportunityApiTests extends IntegrationTestBase {

    private String admin;
    private String user;

    private String admin() {
        if (admin == null) admin = adminToken("admin@test.dev");
        return admin;
    }

    private String user() {
        if (user == null) user = register("user@test.dev");
        return user;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> create(Map<String, Object> body, String token) {
        return rest.exchange(
                "/api/opportunities",
                HttpMethod.POST,
                new HttpEntity<>(body, headers(token)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> list(String query) {
        return rest.exchange(
                "/api/opportunities" + (query == null ? "" : query),
                HttpMethod.GET,
                new HttpEntity<>(headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> content(ResponseEntity<Map<String, Object>> response) {
        return (List<Map<String, Object>>) response.getBody().get("content");
    }

    @Test
    void emptyListIsPublic() {
        ResponseEntity<Map<String, Object>> response = list(null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, ((Number) response.getBody().get("totalElements")).longValue());
    }

    @Test
    void adminCanCreateAndItIsListed() {
        ResponseEntity<Map<String, Object>> response =
                create(opportunityBody("Spring Boot Hackathon", "HACKATHON", "2026-12-01T10:00:00", "Java"), admin());

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Spring Boot Hackathon", response.getBody().get("title"));
        assertEquals("HACKATHON", response.getBody().get("type"));

        ResponseEntity<Map<String, Object>> listing = list(null);
        assertEquals(1, ((Number) listing.getBody().get("totalElements")).longValue());
        assertEquals("Spring Boot Hackathon", content(listing).get(0).get("title"));
    }

    @Test
    void nonAdminCannotCreate() {
        assertEquals(403, create(opportunityBody("User Probe", "HACKATHON", null), user()).getStatusCode().value());
    }

    @Test
    void anonymousCannotCreate() {
        assertEquals(403, create(opportunityBody("Anon Probe", "HACKATHON", null), null).getStatusCode().value());
    }

    @Test
    void unknownTypeInBodyIsRejected() {
        assertEquals(400, create(opportunityBody("Bad Type", "BANANA", null), admin()).getStatusCode().value());
    }

    @Test
    void blankTitleIsRejectedWithFieldError() {
        ResponseEntity<Map<String, Object>> response =
                create(opportunityBody("", "HACKATHON", null), admin());

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().containsKey("errors"), "expected an errors map in the response");
    }

    @Test
    void filtersByTypeTagAndKeywordCaseInsensitively() {
        create(opportunityBody("Spring Boot Hackathon", "HACKATHON", "2026-12-01T10:00:00", "Java"), admin());
        create(opportunityBody("Backend Internship", "INTERNSHIP", "2026-11-01T10:00:00", "Java", "Remote"), admin());
        create(opportunityBody("Docs Sprint", "OPEN_SOURCE", "2026-10-01T10:00:00", "Docs"), admin());

        assertEquals(1, content(list("?type=HACKATHON")).size());
        assertEquals(2, content(list("?tag=java")).size());
        assertEquals(1, content(list("?keyword=spring")).size());
        assertEquals(1, content(list("?keyword=INTERNSHIP")).size());
        assertEquals(1, content(list("?type=OPEN_SOURCE&tag=docs&keyword=sprint")).size());
    }

    @Test
    void keywordWildcardsAreEscaped() {
        create(opportunityBody("Spring Boot Hackathon", "HACKATHON", null), admin());

        // "%" as a keyword must search for the literal character, not match everything.
        assertEquals(0, content(list("?keyword=%25")).size());
    }

    @Test
    void unknownTypeFilterIsRejected() {
        assertEquals(400, list("?type=BANANA").getStatusCode().value());
    }

    @Test
    void pageSizeIsCappedAndNegativePageIsClamped() {
        for (int i = 1; i <= 3; i++) {
            create(opportunityBody("Opportunity " + i, "HACKATHON", null), admin());
        }

        ResponseEntity<Map<String, Object>> oversized = list("?size=1000000");
        assertEquals(50, ((Number) oversized.getBody().get("size")).intValue());

        ResponseEntity<Map<String, Object>> negative = list("?page=-5");
        assertEquals(200, negative.getStatusCode().value());
        assertEquals(0, ((Number) negative.getBody().get("page")).intValue());
        assertEquals(3, ((Number) negative.getBody().get("totalElements")).longValue());
    }

    @Test
    void getUnknownIdIsNotFound() {
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/api/opportunities/99999",
                HttpMethod.GET,
                new HttpEntity<>(headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateReplacesFieldsAndTags() {
        Long id = createOpportunity(admin(),
                opportunityBody("Original Title", "PROGRAM", "2026-12-01T10:00:00", "Old"));

        Map<String, Object> updated = opportunityBody("Updated Title", "INTERNSHIP", "2027-01-15T09:00:00", "New");
        ResponseEntity<Map<String, Object>> response = rest.exchange(
                "/api/opportunities/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updated, headers(admin())),
                (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Updated Title", response.getBody().get("title"));
        assertEquals("INTERNSHIP", response.getBody().get("type"));
        Set<String> tagNames = ((List<Map<String, Object>>) response.getBody().get("tags"))
                .stream().map(t -> (String) t.get("name")).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("New"), tagNames);
    }

    @Test
    void nonAdminCannotUpdateOrDelete() {
        Long id = createOpportunity(admin(), opportunityBody("Locked", "PROGRAM", null));

        assertEquals(403, rest.exchange("/api/opportunities/" + id, HttpMethod.PUT,
                new HttpEntity<>(opportunityBody("Hacked", "PROGRAM", null), headers(user())),
                (Class<Map<String, Object>>) (Class<?>) Map.class).getStatusCode().value());
        assertEquals(403, rest.exchange("/api/opportunities/" + id, HttpMethod.DELETE,
                new HttpEntity<>(headers(user())), Void.class).getStatusCode().value());
    }

    @Test
    void adminCanDeleteAndItDisappears() {
        Long id = createOpportunity(admin(), opportunityBody("Doomed", "PROGRAM", null));

        ResponseEntity<Void> delete = rest.exchange(
                "/api/opportunities/" + id, HttpMethod.DELETE,
                new HttpEntity<>(headers(admin())), Void.class);

        assertEquals(204, delete.getStatusCode().value());
        assertEquals(404, rest.exchange("/api/opportunities/" + id, HttpMethod.GET,
                new HttpEntity<>(headers(null)),
                (Class<Map<String, Object>>) (Class<?>) Map.class).getStatusCode().value());
    }
}
