package com.developershub.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookmark")
@IdClass(BookmarkId.class)
public class Bookmark {

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Bookmark() {
    }

    public Bookmark(User user, Opportunity opportunity) {
        this.user = user;
        this.opportunity = opportunity;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
