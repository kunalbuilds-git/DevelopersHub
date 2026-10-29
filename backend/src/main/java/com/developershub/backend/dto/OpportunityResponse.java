package com.developershub.backend.dto;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityStatus;
import com.developershub.backend.entity.Tag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Stable API shape for opportunities - decouples clients from the JPA entity.
 * daysUntilDeadline lets clients highlight closing opportunities without
 * doing date math in the browser.
 */
public record OpportunityResponse(
        Long id,
        String title,
        String organization,
        String type,
        String description,
        String url,
        LocalDateTime deadline,
        OpportunityStatus status,
        LocalDateTime createdAt,
        Long daysUntilDeadline,
        Set<TagResponse> tags) {

    public record TagResponse(Long id, String name) {
        public static TagResponse from(Tag tag) {
            return new TagResponse(tag.getId(), tag.getName());
        }
    }

    public static OpportunityResponse from(Opportunity opportunity) {
        Set<TagResponse> tags = opportunity.getTags().stream()
                .map(TagResponse::from)
                .collect(java.util.stream.Collectors.toSet());
        Long daysUntilDeadline = opportunity.getDeadline() == null ? null : ChronoUnit.DAYS.between(
                LocalDate.now(),
                opportunity.getDeadline().toLocalDate());
        return new OpportunityResponse(
                opportunity.getId(),
                opportunity.getTitle(),
                opportunity.getOrganization(),
                opportunity.getType(),
                opportunity.getDescription(),
                opportunity.getUrl(),
                opportunity.getDeadline(),
                opportunity.getStatus(),
                opportunity.getCreatedAt(),
                daysUntilDeadline,
                tags);
    }
}
