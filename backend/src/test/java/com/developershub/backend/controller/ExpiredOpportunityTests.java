package com.developershub.backend.controller;

import com.developershub.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #34 - past-deadline opportunities are hidden from the public listing
 * unless the caller asks for them, and opportunities without a deadline are
 * never treated as expired.
 */
class ExpiredOpportunityTests extends IntegrationTestBase {

    private String admin;

    @BeforeEach
    void seedDeadlines() {
        admin = adminToken("admin@test.dev");

        Map<String, Object> expired = opportunityBody("Expired Hackathon", "HACKATHON",
                LocalDateTime.now().minusDays(5).withSecond(0).withNano(0).toString(), "old");
        createOpportunity(admin, expired);

        createOpportunity(admin, opportunityBody("Future Hackathon", "HACKATHON",
                LocalDateTime.now().plusDays(30).withSecond(0).withNano(0).toString(), "new"));

        createOpportunity(admin, opportunityBody("No Deadline Program", "PROGRAM", null, "open"));
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
    private List<Map<String, Object>> content(String query) {
        return (List<Map<String, Object>>) list(query).getBody().get("content");
    }

    @Test
    void expiredOpportunitiesAreHiddenByDefault() {
        List<Map<String, Object>> visible = content(null);

        assertEquals(2, visible.size(), "only the future and the no-deadline opportunity should show");
        assertTrue(visible.stream().noneMatch(o -> "Expired Hackathon".equals(o.get("title"))));
        assertTrue(visible.stream().anyMatch(o -> "Future Hackathon".equals(o.get("title"))));
    }

    @Test
    void opportunitiesWithoutADeadlineAreNeverExpired() {
        assertTrue(content(null).stream().anyMatch(o -> "No Deadline Program".equals(o.get("title"))));
    }

    @Test
    void includeExpiredReturnsEverything() {
        ResponseEntity<Map<String, Object>> response = list("?includeExpired=true");

        assertEquals(3, ((Number) response.getBody().get("totalElements")).longValue());
        assertTrue(content("?includeExpired=true").stream()
                .anyMatch(o -> "Expired Hackathon".equals(o.get("title"))));
    }

    @Test
    void expiredFilterComposesWithTypeTagAndKeyword() {
        assertEquals(1, content("?type=HACKATHON&tag=new").size());
        assertEquals(0, content("?keyword=expired").size(), "the expired one is filtered out by default");
        assertEquals(1, content("?keyword=expired&includeExpired=true").size());
        assertEquals(1, content("?includeExpired=true&tag=old").size(),
                "tag filter applies to expired items too");
    }

    @Test
    void totalsReflectTheExpiryFilter() {
        assertEquals(2, ((Number) list(null).getBody().get("totalElements")).longValue());
        assertEquals(3, ((Number) list("?includeExpired=true").getBody().get("totalElements")).longValue());
    }
}
