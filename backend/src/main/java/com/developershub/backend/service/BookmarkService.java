package com.developershub.backend.service;

import com.developershub.backend.dto.BookmarkResponse;
import com.developershub.backend.entity.Bookmark;
import com.developershub.backend.entity.BookmarkId;
import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.User;
import com.developershub.backend.repository.BookmarkRepository;
import com.developershub.backend.repository.OpportunityRepository;
import com.developershub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final OpportunityRepository opportunityRepository;

    @Autowired
    public BookmarkService(BookmarkRepository bookmarkRepository, UserRepository userRepository, OpportunityRepository opportunityRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.userRepository = userRepository;
        this.opportunityRepository = opportunityRepository;
    }

    public void addBookmark(String userEmail, Long opportunityId) {
        User user = findUser(userEmail);
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opportunity not found"));

        BookmarkId id = new BookmarkId(user.getId(), opportunity.getId());
        if (bookmarkRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already bookmarked");
        }

        bookmarkRepository.save(new Bookmark(user, opportunity));
    }

    public void removeBookmark(String userEmail, Long opportunityId) {
        User user = findUser(userEmail);
        BookmarkId id = new BookmarkId(user.getId(), opportunityId);

        if (!bookmarkRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bookmark not found");
        }
        bookmarkRepository.deleteById(id);
    }

    public List<BookmarkResponse> getBookmarksForUser(String userEmail) {
        return bookmarkRepository.findByUserEmailOrderByOpportunityDeadline(userEmail)
                .stream()
                .map(BookmarkResponse::new)
                .collect(Collectors.toList());
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
