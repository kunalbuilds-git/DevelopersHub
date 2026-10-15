package com.developershub.backend.repository;

import com.developershub.backend.entity.Bookmark;
import com.developershub.backend.entity.BookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {

    List<Bookmark> findByUserEmail(String email);

    @Query("SELECT b FROM Bookmark b WHERE b.user.email = :email ORDER BY b.opportunity.deadline ASC NULLS LAST")
    List<Bookmark> findByUserEmailOrderByOpportunityDeadline(@Param("email") String email);
}
