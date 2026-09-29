package com.developershub.backend.service;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.Tag;
import com.developershub.backend.repository.OpportunityRepository;
import com.developershub.backend.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.Set;

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

    public Page<Opportunity> findAll(String type, String tag, String keyword, Pageable pageable) {
        Specification<Opportunity> spec = Specification
                .where(hasType(type))
                .and(hasTag(tag))
                .and(hasKeyword(keyword));
        return repository.findAll(spec, pageable);
    }

    public Opportunity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Opportunity not found with id " + id));
    }

    public Opportunity create(Opportunity opportunity, Set<String> tagNames) {
        opportunity.setTags(resolveTags(tagNames));
        return repository.save(opportunity);
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
        return tagRepository.findByName(tagName)
                .orElseGet(() -> tagRepository.save(new Tag(tagName)));
    }
}
