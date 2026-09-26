package com.developershub.backend.service;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.repository.OpportunityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class OpportunityService {

    private final OpportunityRepository repository;

    @Autowired
    public OpportunityService(OpportunityRepository repository) {
        this.repository = repository;
    }

    public Page<Opportunity> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Opportunity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Opportunity not found with id " + id));
    }

    public Opportunity create(Opportunity opportunity) {
        return repository.save(opportunity);
    }

    public Opportunity update(Long id, Opportunity updated) {
        Opportunity existing = findById(id);
        existing.setTitle(updated.getTitle());
        existing.setOrganization(updated.getOrganization());
        existing.setType(updated.getType());
        existing.setDescription(updated.getDescription());
        existing.setUrl(updated.getUrl());
        existing.setDeadline(updated.getDeadline());
        return repository.save(existing);
    }

    public void delete(Long id) {
        Opportunity existing = findById(id);
        repository.delete(existing);
    }
}
