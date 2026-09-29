package com.developershub.backend.specification;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.Tag;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;

public class OpportunitySpecifications {

    public static Specification<Opportunity> hasType(String type) {
        return (root, query, cb) ->
            type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Opportunity> hasTag(String tagName) {
        return (root, query, cb) -> {
            if (tagName == null) {
                return null;
            }
            Join<Opportunity, Tag> tagJoin = root.join("tags");
            return cb.equal(tagJoin.get("name"), tagName);
        };
    }

    public static Specification<Opportunity> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("organization")), pattern)
            );
        };
    }
}
