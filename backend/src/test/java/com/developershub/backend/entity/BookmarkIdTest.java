package com.developershub.backend.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkIdTest {

    @Test
    void equalsIsValueBased() {
        BookmarkId first = new BookmarkId(1L, 2L);
        BookmarkId second = new BookmarkId(1L, 2L);

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void aDifferentOpportunityMakesTheIdsUnequal() {
        BookmarkId first = new BookmarkId(1L, 2L);
        BookmarkId second = new BookmarkId(1L, 3L);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void aDifferentUserMakesTheIdsUnequal() {
        BookmarkId first = new BookmarkId(1L, 2L);
        BookmarkId second = new BookmarkId(9L, 2L);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void nullComponentsAreHandledWithoutThrowing() {
        BookmarkId first = new BookmarkId(null, null);
        BookmarkId second = new BookmarkId(null, null);

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first).isNotEqualTo(new BookmarkId(1L, 2L));
    }

    @Test
    void anotherTypeAndNullAreNeverEqual() {
        BookmarkId id = new BookmarkId(1L, 2L);

        assertThat(id.equals(null)).isFalse();
        assertThat(id.equals("1-2")).isFalse();
        assertThat(id.equals(id)).isTrue();
    }
}
