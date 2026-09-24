package com.developershub.backend.controller;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.service.OpportunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final OpportunityService service;

    @Autowired
    public OpportunityController(OpportunityService service) {
        this.service = service;
    }

    @GetMapping
    public List<Opportunity> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Opportunity getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Opportunity create(@RequestBody Opportunity opportunity) {
        return service.create(opportunity);
    }

    @PutMapping("/{id}")
    public Opportunity update(@PathVariable Long id, @RequestBody Opportunity opportunity) {
        return service.update(id, opportunity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}