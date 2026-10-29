package com.developershub.backend.dto;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.Tag;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Stable API shape for opportunities - decouples clients from the JPA entity.
 */
public record OpportunityResponse(
        Long id,
        String title,
        String organization,
        String type,
        String description,
        String url,
        LocalDateTime deadline,
        LocalDateTime createdAt,
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
        return new OpportunityResponse(
                opportunity.getId(),
                opportunity.getTitle(),
                opportunity.getOrganization(),
                opportunity.getType(),
                opportunity.getDescription(),
                opportunity.getUrl(),
                opportunity.getDeadline(),
                opportunity.getCreatedAt(),
                tags);
    }
}
