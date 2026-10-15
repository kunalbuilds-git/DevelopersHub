package com.developershub.backend.dto;

import com.developershub.backend.entity.Bookmark;

import java.time.LocalDateTime;

public class BookmarkResponse {

    private Long opportunityId;
    private String title;
    private String organization;
    private String type;
    private LocalDateTime deadline;
    private LocalDateTime bookmarkedAt;

    public BookmarkResponse(Bookmark bookmark) {
        this.opportunityId = bookmark.getOpportunity().getId();
        this.title = bookmark.getOpportunity().getTitle();
        this.organization = bookmark.getOpportunity().getOrganization();
        this.type = bookmark.getOpportunity().getType();
        this.deadline = bookmark.getOpportunity().getDeadline();
        this.bookmarkedAt = bookmark.getCreatedAt();
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public String getTitle() {
        return title;
    }

    public String getOrganization() {
        return organization;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public LocalDateTime getBookmarkedAt() {
        return bookmarkedAt;
    }
}
