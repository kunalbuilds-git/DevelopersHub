package com.developershub.backend.dto;

import com.developershub.backend.entity.Opportunity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class OpportunityRequest {

    private String title;
    private String organization;
    private String type;
    private String description;
    private String url;
    private LocalDateTime deadline;
    private Set<String> tags = new HashSet<>();

    public Opportunity toOpportunity() {
        Opportunity opportunity = new Opportunity();
        opportunity.setTitle(title);
        opportunity.setOrganization(organization);
        opportunity.setType(type);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
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
