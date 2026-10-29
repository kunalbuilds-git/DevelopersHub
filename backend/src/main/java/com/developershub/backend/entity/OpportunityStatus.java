package com.developershub.backend.entity;

/**
 * Lifecycle of an opportunity. Public listings only show APPROVED;
 * user submissions start as PENDING and are curated by admins.
 */
public enum OpportunityStatus {
    PENDING,
    APPROVED,
    REJECTED
}
