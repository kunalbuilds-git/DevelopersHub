package com.developershub.backend.controller;

import com.developershub.backend.dto.OpportunityRequest;
import com.developershub.backend.dto.OpportunityResponse;
import com.developershub.backend.dto.PageResponse;
import com.developershub.backend.entity.OpportunityLevel;
import com.developershub.backend.entity.OpportunityStatus;
import com.developershub.backend.entity.OpportunityType;
import com.developershub.backend.entity.User;
import com.developershub.backend.repository.UserRepository;
import com.developershub.backend.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/api/opportunities")
@Tag(name = "Opportunities", description = "Browse, submit, and curate opportunities")
public class OpportunityController {

    private static final int MAX_PAGE_SIZE = 50;

    private final OpportunityService service;
    private final UserRepository userRepository;

    @Autowired
    public OpportunityController(OpportunityService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    /** Public, curated listing - only APPROVED opportunities are returned. */
    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List opportunities", description = "Public listing of approved opportunities. "
            + "Past-deadline opportunities are hidden unless includeExpired=true.")
    public PageResponse<OpportunityResponse> getAll(
            @Parameter(description = "Filter by type") @RequestParam(required = false) String type,
            @Parameter(description = "Filter by experience level") @RequestParam(required = false) String level,
            @Parameter(description = "Filter by tag") @RequestParam(required = false) String tag,
            @Parameter(description = "Keyword search across title and organization")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Include opportunities whose deadline has passed")
            @RequestParam(defaultValue = "false") boolean includeExpired,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        OpportunityType typeFilter = parseEnum(type, OpportunityType.class, "type");
        OpportunityLevel levelFilter = parseEnum(level, OpportunityLevel.class, "level");
        PageRequest pageRequest = pageRequest(page, size);
        return PageResponse.of(
                service.findAll(typeFilter, levelFilter, tag, keyword, includeExpired, pageRequest),
                OpportunityResponse::from);
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Get one opportunity")
    @ApiResponses(@ApiResponse(responseCode = "404", description = "No approved opportunity with that id",
            content = @Content))
    public OpportunityResponse getById(@PathVariable Long id) {
        return OpportunityResponse.from(service.findApprovedById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an opportunity", description = "Admin only. Created opportunities are approved immediately.")
    public OpportunityResponse create(@Valid @RequestBody OpportunityRequest request) {
        return OpportunityResponse.from(service.create(request.toOpportunity(), request.getTags()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an opportunity", description = "Admin only.")
    public OpportunityResponse update(@PathVariable Long id, @Valid @RequestBody OpportunityRequest request) {
        return OpportunityResponse.from(service.update(id, request.toOpportunity(), request.getTags()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an opportunity", description = "Admin only.")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /** Any logged-in user can submit an opportunity; it stays hidden until reviewed. */
    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit an opportunity", description = "Creates a PENDING submission that an admin reviews.")
    public OpportunityResponse submit(@Valid @RequestBody OpportunityRequest request, Authentication authentication) {
        User submitter = currentUser(authentication);
        return OpportunityResponse.from(service.submit(request.toOpportunity(), request.getTags(), submitter));
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Review queue", description = "Admin only. Lists submissions by status.")
    public PageResponse<OpportunityResponse> submissions(
            @Parameter(description = "PENDING, APPROVED, or REJECTED") @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        OpportunityStatus statusFilter = parseStatus(status);
        return PageResponse.of(
                service.findSubmissions(statusFilter, pageRequest(page, size)),
                OpportunityResponse::from);
    }

    /**
     * The signed-in user's own submissions. Deliberately a literal path so it
     * never collides with the admin queue (/submissions) or the review actions
     * (/submissions/{id}/approve).
     */
    @GetMapping("/submissions/mine")
    @Operation(summary = "My submissions", description = "Lists only the signed-in user's submissions, with their status.")
    public PageResponse<OpportunityResponse> mySubmissions(
            @Parameter(description = "Optional status filter") @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = currentUser(authentication);
        OpportunityStatus statusFilter = status == null || status.isBlank()
                ? null
                : parseEnum(status, OpportunityStatus.class, "status");
        return PageResponse.of(
                service.findMySubmissions(user, statusFilter, pageRequest(page, size)),
                OpportunityResponse::from);
    }

    @PostMapping("/submissions/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a submission", description = "Admin only. Publishes the submission.")
    public OpportunityResponse approve(@PathVariable Long id) {
        return OpportunityResponse.from(service.approve(id));
    }

    @PostMapping("/submissions/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a submission", description = "Admin only.")
    public OpportunityResponse reject(@PathVariable Long id) {
        return OpportunityResponse.from(service.reject(id));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private OpportunityStatus parseStatus(String status) {
        return status == null || status.isBlank()
                ? OpportunityStatus.PENDING
                : parseEnum(status, OpportunityStatus.class, "status");
    }

    private <E extends Enum<E>> E parseEnum(String raw, Class<E> enumClass, String paramName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String allowed = String.join(", ",
                    java.util.Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toArray(String[]::new));
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unknown " + paramName + ": " + raw + ". Allowed: " + allowed);
        }
    }
}
