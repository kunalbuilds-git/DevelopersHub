package com.developershub.backend.service;

import com.developershub.backend.dto.BookmarkResponse;
import com.developershub.backend.entity.Bookmark;
import com.developershub.backend.entity.BookmarkId;
import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityStatus;
import com.developershub.backend.entity.User;
import com.developershub.backend.repository.BookmarkRepository;
import com.developershub.backend.repository.OpportunityRepository;
import com.developershub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    private static final String EMAIL = "student@example.com";
    private static final Long USER_ID = 1L;
    private static final Long OPPORTUNITY_ID = 10L;

    @Mock
    BookmarkRepository bookmarkRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    OpportunityRepository opportunityRepository;

    private BookmarkService service;

    @BeforeEach
    void setUp() {
        service = new BookmarkService(bookmarkRepository, userRepository, opportunityRepository);
    }

    @Test
    void addBookmark_savesTheBookmarkForAnApprovedOpportunity() {
        User user = user();
        Opportunity opportunity = opportunity(OpportunityStatus.APPROVED);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(opportunityRepository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(opportunity));
        when(bookmarkRepository.existsById(new BookmarkId(USER_ID, OPPORTUNITY_ID))).thenReturn(false);

        service.addBookmark(EMAIL, OPPORTUNITY_ID);

        ArgumentCaptor<Bookmark> captor = ArgumentCaptor.forClass(Bookmark.class);
        verify(bookmarkRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getOpportunity()).isSameAs(opportunity);
    }

    @Test
    void addBookmark_unknownUser_throwsUnauthorized() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addBookmark(EMAIL, OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(opportunityRepository, never()).findById(any());
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void addBookmark_unknownOpportunity_throwsNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(opportunityRepository.findById(OPPORTUNITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addBookmark(EMAIL, OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void addBookmark_pendingOpportunity_throwsNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(opportunityRepository.findById(OPPORTUNITY_ID))
                .thenReturn(Optional.of(opportunity(OpportunityStatus.PENDING)));

        assertThatThrownBy(() -> service.addBookmark(EMAIL, OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void addBookmark_rejectedOpportunity_throwsNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(opportunityRepository.findById(OPPORTUNITY_ID))
                .thenReturn(Optional.of(opportunity(OpportunityStatus.REJECTED)));

        assertThatThrownBy(() -> service.addBookmark(EMAIL, OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void addBookmark_whenAlreadyBookmarked_throwsConflict() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(opportunityRepository.findById(OPPORTUNITY_ID))
                .thenReturn(Optional.of(opportunity(OpportunityStatus.APPROVED)));
        when(bookmarkRepository.existsById(new BookmarkId(USER_ID, OPPORTUNITY_ID))).thenReturn(true);

        assertThatThrownBy(() -> service.addBookmark(EMAIL, OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void removeBookmark_deletesTheStoredBookmark() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(bookmarkRepository.existsById(new BookmarkId(USER_ID, OPPORTUNITY_ID))).thenReturn(true);

        service.removeBookmark(EMAIL, OPPORTUNITY_ID);

        verify(bookmarkRepository).deleteById(new BookmarkId(USER_ID, OPPORTUNITY_ID));
    }

    @Test
    void removeBookmark_whenNotBookmarked_throwsNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(bookmarkRepository.existsById(new BookmarkId(USER_ID, OPPORTUNITY_ID))).thenReturn(false);

        assertThatThrownBy(() -> service.removeBookmark(EMAIL, OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(bookmarkRepository, never()).deleteById(any());
    }

    @Test
    void removeBookmark_unknownUser_throwsUnauthorized() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeBookmark(EMAIL, OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(bookmarkRepository, never()).deleteById(any());
    }

    @Test
    void getBookmarksForUser_mapsTheBookmarksToResponses() {
        LocalDateTime deadline = LocalDateTime.of(2026, 12, 1, 9, 0);
        LocalDateTime bookmarkedAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        Opportunity opportunity = opportunity(OpportunityStatus.APPROVED);
        opportunity.setTitle("Hackathon");
        opportunity.setOrganization("Acme");
        opportunity.setType("HACKATHON");
        opportunity.setDeadline(deadline);

        Bookmark bookmark = mock(Bookmark.class);
        when(bookmark.getOpportunity()).thenReturn(opportunity);
        when(bookmark.getCreatedAt()).thenReturn(bookmarkedAt);
        when(bookmarkRepository.findByUserEmailOrderByOpportunityDeadline(EMAIL)).thenReturn(List.of(bookmark));

        List<BookmarkResponse> responses = service.getBookmarksForUser(EMAIL);

        assertThat(responses).hasSize(1);
        BookmarkResponse response = responses.get(0);
        assertThat(response.getOpportunityId()).isEqualTo(OPPORTUNITY_ID);
        assertThat(response.getTitle()).isEqualTo("Hackathon");
        assertThat(response.getOrganization()).isEqualTo("Acme");
        assertThat(response.getType()).isEqualTo("HACKATHON");
        assertThat(response.getDeadline()).isEqualTo(deadline);
        assertThat(response.getBookmarkedAt()).isEqualTo(bookmarkedAt);
    }

    @Test
    void getBookmarksForUser_withoutBookmarks_returnsAnEmptyList() {
        when(bookmarkRepository.findByUserEmailOrderByOpportunityDeadline(EMAIL)).thenReturn(List.of());

        assertThat(service.getBookmarksForUser(EMAIL)).isEmpty();
    }

    private static User user() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        return user;
    }

    private static Opportunity opportunity(OpportunityStatus status) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(OPPORTUNITY_ID);
        opportunity.setStatus(status);
        return opportunity;
    }
}
