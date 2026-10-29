package com.developershub.backend.service;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityStatus;
import com.developershub.backend.entity.OpportunityType;
import com.developershub.backend.entity.Tag;
import com.developershub.backend.repository.OpportunityRepository;
import com.developershub.backend.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.developershub.backend.specification.OpportunitySpecifications.hasStatus;
import static com.developershub.backend.specification.OpportunitySpecifications.hasType;
import static com.developershub.backend.specification.OpportunitySpecifications.hasTag;
import static com.developershub.backend.specification.OpportunitySpecifications.hasKeyword;

@Service
public class OpportunityService {

    private final OpportunityRepository repository;
    private final TagRepository tagRepository;

    @Autowired
    public OpportunityService(OpportunityRepository repository, TagRepository tagRepository) {
        this.repository = repository;
        this.tagRepository = tagRepository;
    }

    /** Public listing: only curated (APPROVED) opportunities are visible. */
    public Page<Opportunity> findAll(OpportunityType type, String tag, String keyword, Pageable pageable) {
        Specification<Opportunity> spec = Specification
                .where(hasStatus(OpportunityStatus.APPROVED))
                .and(hasType(type))
                .and(hasTag(tag))
                .and(hasKeyword(keyword));
        return repository.findAll(spec, pageable);
    }

    public Opportunity findApprovedById(Long id) {
        return repository.findByIdAndStatus(id, OpportunityStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Opportunity not found with id " + id));
    }

    public Opportunity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Opportunity not found with id " + id));
    }

    /** Admin-created opportunities are approved immediately. */
    public Opportunity create(Opportunity opportunity, Set<String> tagNames) {
        opportunity.setStatus(OpportunityStatus.APPROVED);
        opportunity.setTags(resolveTags(tagNames));
        return repository.save(opportunity);
    }

    /** User submissions wait as PENDING until an admin reviews them. */
    public Opportunity submit(Opportunity opportunity, Set<String> tagNames) {
        opportunity.setStatus(OpportunityStatus.PENDING);
        opportunity.setTags(resolveTags(tagNames));
        return repository.save(opportunity);
    }

    public Page<Opportunity> findSubmissions(OpportunityStatus status, Pageable pageable) {
        Specification<Opportunity> spec = Specification.where(hasStatus(status));
        return repository.findAll(spec, pageable);
    }

    public Opportunity approve(Long id) {
        return transition(id, OpportunityStatus.APPROVED);
    }

    public Opportunity reject(Long id) {
        return transition(id, OpportunityStatus.REJECTED);
    }

    public Opportunity update(Long id, Opportunity updated, Set<String> tagNames) {
        Opportunity existing = findById(id);
        existing.setTitle(updated.getTitle());
        existing.setOrganization(updated.getOrganization());
        existing.setType(updated.getType());
        existing.setDescription(updated.getDescription());
        existing.setUrl(updated.getUrl());
        existing.setDeadline(updated.getDeadline());
        if (tagNames != null) {
            existing.setTags(resolveTags(tagNames));
        }
        return repository.save(existing);
    }

    public void delete(Long id) {
        Opportunity existing = findById(id);
        repository.delete(existing);
    }

    private Opportunity transition(Long id, OpportunityStatus target) {
        Opportunity existing = findById(id);
        if (existing.getStatus() != OpportunityStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Submission has already been reviewed");
        }
        existing.setStatus(target);
        return repository.save(existing);
    }

    private Set<Tag> resolveTags(Set<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        if (tagNames != null) {
            for (String name : tagNames) {
                tags.add(findOrCreateTag(name));
            }
        }
        return tags;
    }

    private Tag findOrCreateTag(String tagName) {
        Optional<Tag> existing = tagRepository.findByName(tagName);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            // Flushing inside the try makes a concurrent insert of the same tag
            // surface here as a constraint violation instead of at commit time.
            return tagRepository.saveAndFlush(new Tag(tagName));
        } catch (DataIntegrityViolationException e) {
            return tagRepository.findByName(tagName)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT, "Tag could not be created: " + tagName));
        }
    }
}
