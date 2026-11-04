package com.developershub.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityLevel;
import com.developershub.backend.entity.OpportunityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Schema(description = "Opportunity payload for creating, updating, or submitting")
public class OpportunityRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Title of the opportunity")
    private String title;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Company or community hosting it")
    private String organization;

    @NotNull
    @Schema(description = "Category: HACKATHON, INTERNSHIP, PROGRAM, OPEN_SOURCE, EVENT")
    private OpportunityType type;

    @Schema(description = "Target level: ANY, BEGINNER, INTERMEDIATE, ADVANCED")
    private OpportunityLevel level;

    @Size(max = 10000)
    @Schema(description = "What it is and who it is for")
    private String description;

    @Size(max = 500)
    @Schema(description = "Link to the announcement or application form")
    private String url;

    @Schema(description = "Deadline, in ISO-8601 local date-time format")
    private LocalDateTime deadline;

    @Valid
    private Set<@NotBlank @Size(max = 50) String> tags = new HashSet<>();

    public Opportunity toOpportunity() {
        Opportunity opportunity = new Opportunity();
        opportunity.setTitle(title);
        opportunity.setOrganization(organization);
        opportunity.setType(type.name());
        opportunity.setLevel(level == null ? OpportunityLevel.ANY : level);
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

    public OpportunityLevel getLevel() {
        return level;
    }

    public void setLevel(OpportunityLevel level) {
        this.level = level;
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
