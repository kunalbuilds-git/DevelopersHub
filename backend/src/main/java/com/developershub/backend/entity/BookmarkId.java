package com.developershub.backend.entity;

import java.io.Serializable;
import java.util.Objects;

public class BookmarkId implements Serializable {

    private Long user;
    private Long opportunity;

    public BookmarkId() {
    }

    public BookmarkId(Long user, Long opportunity) {
        this.user = user;
        this.opportunity = opportunity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookmarkId)) return false;
        BookmarkId that = (BookmarkId) o;
        return Objects.equals(user, that.user) && Objects.equals(opportunity, that.opportunity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, opportunity);
    }
}
