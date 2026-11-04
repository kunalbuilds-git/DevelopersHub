package com.developershub.backend.controller;

import com.developershub.backend.dto.BookmarkResponse;
import com.developershub.backend.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@Tag(name = "Bookmarks", description = "Save and manage your bookmarked opportunities")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Autowired
    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/{opportunityId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bookmark an opportunity", description = "Saves an opportunity to your list")
    public void addBookmark(@PathVariable Long opportunityId, Authentication authentication) {
        String email = authentication.getName();
        bookmarkService.addBookmark(email, opportunityId);
    }

    @DeleteMapping("/{opportunityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a bookmark", description = "Removes an opportunity from your saved list")
    public void removeBookmark(@PathVariable Long opportunityId, Authentication authentication) {
        String email = authentication.getName();
        bookmarkService.removeBookmark(email, opportunityId);
    }

    @GetMapping
    @Operation(summary = "My saved list", description = "Your bookmarked opportunities, sorted by deadline")
    public List<BookmarkResponse> getMyBookmarks(Authentication authentication) {
        String email = authentication.getName();
        return bookmarkService.getBookmarksForUser(email);
    }
}
