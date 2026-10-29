package com.developershub.backend.specification;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityStatus;
import com.developershub.backend.entity.OpportunityType;
import com.developershub.backend.entity.Tag;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;

import java.util.Locale;

public class OpportunitySpecifications {

    public static Specification<Opportunity> hasType(OpportunityType type) {
        return (root, query, cb) ->
            type == null ? null : cb.equal(root.get("type"), type.name());
    }

    public static Specification<Opportunity> hasStatus(OpportunityStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Opportunity> hasTag(String tagName) {
        return (root, query, cb) -> {
            if (tagName == null || tagName.isBlank()) {
                return null;
            }
            Join<Opportunity, Tag> tagJoin = root.join("tags");
            return cb.equal(cb.lower(tagJoin.get("name")), tagName.toLowerCase(Locale.ROOT));
        };
    }

    public static Specification<Opportunity> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + escapeWildcards(keyword.toLowerCase(Locale.ROOT)) + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("organization")), pattern, '\\')
            );
        };
    }

    /**
     * Escapes LIKE wildcards so a keyword of "%" or "_" searches for the literal
     * character instead of matching everything.
     */
    private static String escapeWildcards(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
