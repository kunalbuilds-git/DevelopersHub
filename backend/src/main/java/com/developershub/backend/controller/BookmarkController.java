package com.developershub.backend.controller;

import com.developershub.backend.entity.Bookmark;
import com.developershub.backend.service.BookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Autowired
    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/{opportunityId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void addBookmark(@PathVariable Long opportunityId, Authentication authentication) {
        String email = authentication.getName();
        bookmarkService.addBookmark(email, opportunityId);
    }

    @DeleteMapping("/{opportunityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(@PathVariable Long opportunityId, Authentication authentication) {
        String email = authentication.getName();
        bookmarkService.removeBookmark(email, opportunityId);
    }

    @GetMapping
    public List<Bookmark> getMyBookmarks(Authentication authentication) {
        String email = authentication.getName();
        return bookmarkService.getBookmarksForUser(email);
    }
}
