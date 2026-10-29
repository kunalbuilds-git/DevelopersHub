package com.developershub.backend.dto;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class OpportunityRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 255)
    private String organization;

    @NotNull
    private OpportunityType type;

    @Size(max = 10000)
    private String description;

    @Size(max = 500)
    private String url;

    private LocalDateTime deadline;

    @Valid
    private Set<@NotBlank @Size(max = 50) String> tags = new HashSet<>();

    public Opportunity toOpportunity() {
        Opportunity opportunity = new Opportunity();
        opportunity.setTitle(title);
        opportunity.setOrganization(organization);
        opportunity.setType(type.name());
        opportunity.setDescription(description);
        opportunity.setUrl(url);
        opportunity.setDeadline(deadline);
        return opportunity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public OpportunityType getType() {
        return type;
    }

    public void setType(OpportunityType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
