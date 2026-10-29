package com.developershub.backend.controller;

import com.developershub.backend.dto.OpportunityRequest;
import com.developershub.backend.dto.OpportunityResponse;
import com.developershub.backend.dto.PageResponse;
import com.developershub.backend.entity.OpportunityStatus;
import com.developershub.backend.entity.OpportunityType;
import com.developershub.backend.service.OpportunityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private static final int MAX_PAGE_SIZE = 50;

    private final OpportunityService service;

    @Autowired
    public OpportunityController(OpportunityService service) {
        this.service = service;
    }

    /** Public, curated listing - only APPROVED opportunities are returned. */
    @GetMapping
    public PageResponse<OpportunityResponse> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        OpportunityType typeFilter = parseType(type);
        PageRequest pageRequest = pageRequest(page, size);
        return PageResponse.of(service.findAll(typeFilter, tag, keyword, pageRequest), OpportunityResponse::from);
    }

    @GetMapping("/{id}")
    public OpportunityResponse getById(@PathVariable Long id) {
        return OpportunityResponse.from(service.findApprovedById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public OpportunityResponse create(@Valid @RequestBody OpportunityRequest request) {
        return OpportunityResponse.from(service.create(request.toOpportunity(), request.getTags()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public OpportunityResponse update(@PathVariable Long id, @Valid @RequestBody OpportunityRequest request) {
        return OpportunityResponse.from(service.update(id, request.toOpportunity(), request.getTags()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /** Any logged-in user can submit an opportunity; it stays hidden until reviewed. */
    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public OpportunityResponse submit(@Valid @RequestBody OpportunityRequest request) {
        return OpportunityResponse.from(service.submit(request.toOpportunity(), request.getTags()));
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<OpportunityResponse> submissions(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        OpportunityStatus statusFilter = parseStatus(status);
        return PageResponse.of(
                service.findSubmissions(statusFilter, pageRequest(page, size)),
                OpportunityResponse::from);
    }

    @PostMapping("/submissions/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public OpportunityResponse approve(@PathVariable Long id) {
        return OpportunityResponse.from(service.approve(id));
    }

    @PostMapping("/submissions/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public OpportunityResponse reject(@PathVariable Long id) {
        return OpportunityResponse.from(service.reject(id));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private OpportunityType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return OpportunityType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown type: " + type + ". Allowed: HACKATHON, INTERNSHIP, PROGRAM, OPEN_SOURCE");
        }
    }

    private OpportunityStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return OpportunityStatus.PENDING;
        }
        try {
            return OpportunityStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown status: " + status + ". Allowed: PENDING, APPROVED, REJECTED");
        }
    }
}
