package com.developershub.backend.service;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityLevel;
import com.developershub.backend.entity.OpportunityStatus;
import com.developershub.backend.entity.OpportunityType;
import com.developershub.backend.entity.Tag;
import com.developershub.backend.entity.User;
import com.developershub.backend.repository.OpportunityRepository;
import com.developershub.backend.repository.TagRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    private static final Long OPPORTUNITY_ID = 42L;

    @Mock
    OpportunityRepository repository;
    @Mock
    TagRepository tagRepository;

    private OpportunityService service;

    @BeforeEach
    void setUp() {
        service = new OpportunityService(repository, tagRepository);
    }

    @Test
    void findAll_buildsTheSpecificationChainAndDelegatesToTheRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Opportunity> expected = new PageImpl<>(List.of(opportunity(OPPORTUNITY_ID, OpportunityStatus.APPROVED)));
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<Opportunity> actual = service.findAll(OpportunityType.HACKATHON, OpportunityLevel.BEGINNER,
                "java", "bootcamp", false, pageable);

        assertThat(actual).isSameAs(expected);

        Root<Opportunity> root = evaluate(capturedSpecification(pageable));
        verify(root).get("status");
        verify(root).get("type");
        verify(root).get("level");
        verify(root).join("tags");
        verify(root).get("title");
        verify(root).get("organization");
    }

    @Test
    void findAll_filtersOutExpiredOpportunitiesWhenIncludeExpiredIsFalse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.findAll(null, null, null, null, false, pageable);

        Root<Opportunity> root = evaluate(capturedSpecification(pageable));
        verify(root, atLeastOnce()).get("deadline");
    }

    @Test
    void findAll_keepsExpiredOpportunitiesWhenIncludeExpiredIsTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.findAll(null, null, null, null, true, pageable);

        Root<Opportunity> root = evaluate(capturedSpecification(pageable));
        verify(root, never()).get("deadline");
    }

    @Test
    void findAll_omitsTheFiltersThatHaveNoValue() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.findAll(null, null, null, null, true, pageable);

        Root<Opportunity> root = evaluate(capturedSpecification(pageable));
        verify(root).get("status");
        verify(root, never()).get("type");
        verify(root, never()).get("level");
        verify(root, never()).join("tags");
        verify(root, never()).get("title");
        verify(root, never()).get("organization");
    }

    @Test
    void findApprovedById_returnsTheApprovedOpportunity() {
        Opportunity approved = opportunity(OPPORTUNITY_ID, OpportunityStatus.APPROVED);
        when(repository.findByIdAndStatus(OPPORTUNITY_ID, OpportunityStatus.APPROVED)).thenReturn(Optional.of(approved));

        assertThat(service.findApprovedById(OPPORTUNITY_ID)).isSameAs(approved);
    }

    @Test
    void findApprovedById_whenMissing_throwsNotFound() {
        when(repository.findByIdAndStatus(OPPORTUNITY_ID, OpportunityStatus.APPROVED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findApprovedById(OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void findById_returnsTheOpportunity() {
        Opportunity pending = opportunity(OPPORTUNITY_ID, OpportunityStatus.PENDING);
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(pending));

        assertThat(service.findById(OPPORTUNITY_ID)).isSameAs(pending);
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void create_forcesApprovedStatusAndResolvesTheTags() {
        Tag java = tag(1L, "java");
        when(tagRepository.findByName("java")).thenReturn(Optional.of(java));
        when(repository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Opportunity opportunity = new Opportunity();

        Opportunity saved = service.create(opportunity, Set.of("java"));

        assertThat(saved.getStatus()).isEqualTo(OpportunityStatus.APPROVED);
        assertThat(saved.getTags()).containsExactly(java);
        verify(tagRepository, never()).saveAndFlush(any());
        verify(repository).save(opportunity);
    }

    @Test
    void create_createsAMissingTagThroughSaveAndFlush() {
        Tag created = tag(7L, "java");
        when(tagRepository.findByName("java")).thenReturn(Optional.empty());
        when(tagRepository.saveAndFlush(any(Tag.class))).thenReturn(created);
        when(repository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Opportunity saved = service.create(new Opportunity(), Set.of("java"));

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).saveAndFlush(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getName()).isEqualTo("java");
        assertThat(saved.getTags()).containsExactly(created);
    }

    @Test
    void create_fallsBackToTheTagCreatedByAConcurrentInsert() {
        Tag concurrent = tag(9L, "java");
        when(tagRepository.findByName("java")).thenReturn(Optional.empty()).thenReturn(Optional.of(concurrent));
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));
        when(repository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Opportunity saved = service.create(new Opportunity(), Set.of("java"));

        assertThat(saved.getTags()).containsExactly(concurrent);
    }

    @Test
    void create_whenTheConcurrentTagCannotBeReRead_throwsConflict() {
        when(tagRepository.findByName("java")).thenReturn(Optional.empty()).thenReturn(Optional.empty());
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> service.create(new Opportunity(), Set.of("java")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void submit_forcesPendingStatusAndRecordsTheSubmitter() {
        User submitter = new User();
        submitter.setId(3L);
        when(repository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Opportunity opportunity = new Opportunity();

        Opportunity saved = service.submit(opportunity, Set.of(), submitter);

        assertThat(saved.getStatus()).isEqualTo(OpportunityStatus.PENDING);
        assertThat(saved.getSubmittedBy()).isSameAs(submitter);
        verify(repository).save(opportunity);
    }

    @Test
    void update_copiesTheSubmittedFieldsOntoTheExistingOpportunity() {
        LocalDateTime deadline = LocalDateTime.of(2026, 12, 1, 9, 0);
        Tag rust = tag(2L, "rust");
        Opportunity existing = opportunity(OPPORTUNITY_ID, OpportunityStatus.APPROVED);
        existing.setTags(Set.of(tag(1L, "java")));
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(existing));
        when(tagRepository.findByName("rust")).thenReturn(Optional.of(rust));
        when(repository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Opportunity patch = new Opportunity();
        patch.setTitle("New title");
        patch.setOrganization("New org");
        patch.setType("HACKATHON");
        patch.setDescription("New description");
        patch.setUrl("https://example.com/new");
        patch.setDeadline(deadline);

        service.update(OPPORTUNITY_ID, patch, Set.of("rust"));

        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getOrganization()).isEqualTo("New org");
        assertThat(existing.getType()).isEqualTo("HACKATHON");
        assertThat(existing.getDescription()).isEqualTo("New description");
        assertThat(existing.getUrl()).isEqualTo("https://example.com/new");
        assertThat(existing.getDeadline()).isEqualTo(deadline);
        assertThat(existing.getTags()).containsExactly(rust);
        verify(repository).save(existing);
    }

    @Test
    void update_withNullTags_leavesTheExistingTagsUntouched() {
        Tag java = tag(1L, "java");
        Opportunity existing = opportunity(OPPORTUNITY_ID, OpportunityStatus.APPROVED);
        existing.setTags(Set.of(java));
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(OPPORTUNITY_ID, new Opportunity(), null);

        assertThat(existing.getTags()).containsExactly(java);
        verify(tagRepository, never()).findByName(anyString());
        verify(tagRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_whenMissing_throwsNotFound() {
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(OPPORTUNITY_ID, new Opportunity(), Set.of()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(repository, never()).save(any());
    }

    @Test
    void delete_removesTheExistingOpportunity() {
        Opportunity existing = opportunity(OPPORTUNITY_ID, OpportunityStatus.APPROVED);
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(existing));

        service.delete(OPPORTUNITY_ID);

        verify(repository).delete(existing);
    }

    @Test
    void delete_whenMissing_throwsNotFound() {
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(repository, never()).delete(any(Opportunity.class));
    }

    @Test
    void approve_transitionsAPendingSubmissionToApproved() {
        Opportunity pending = opportunity(OPPORTUNITY_ID, OpportunityStatus.PENDING);
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(pending));
        when(repository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Opportunity saved = service.approve(OPPORTUNITY_ID);

        assertThat(saved.getStatus()).isEqualTo(OpportunityStatus.APPROVED);
        verify(repository).save(pending);
    }

    @Test
    void reject_transitionsAPendingSubmissionToRejected() {
        Opportunity pending = opportunity(OPPORTUNITY_ID, OpportunityStatus.PENDING);
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(pending));
        when(repository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Opportunity saved = service.reject(OPPORTUNITY_ID);

        assertThat(saved.getStatus()).isEqualTo(OpportunityStatus.REJECTED);
        verify(repository).save(pending);
    }

    @Test
    void approve_whenTheSubmissionWasAlreadyReviewed_throwsConflict() {
        Opportunity approved = opportunity(OPPORTUNITY_ID, OpportunityStatus.APPROVED);
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.approve(OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(repository, never()).save(any());
    }

    @Test
    void reject_whenTheSubmissionWasAlreadyReviewed_throwsConflict() {
        Opportunity rejected = opportunity(OPPORTUNITY_ID, OpportunityStatus.REJECTED);
        when(repository.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> service.reject(OPPORTUNITY_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(repository, never()).save(any());
    }

    @Test
    void findMySubmissions_scopesTheSpecificationToTheUser() {
        Pageable pageable = PageRequest.of(0, 5);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));
        User user = new User();
        user.setId(3L);

        service.findMySubmissions(user, OpportunityStatus.PENDING, pageable);

        Root<Opportunity> root = evaluate(capturedSpecification(pageable));
        verify(root).get("submittedBy");
        verify(root).get("status");
    }

    @Test
    void findSubmissions_scopesTheSpecificationToTheStatus() {
        Pageable pageable = PageRequest.of(0, 5);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service.findSubmissions(OpportunityStatus.PENDING, pageable);

        Root<Opportunity> root = evaluate(capturedSpecification(pageable));
        verify(root).get("status");
    }

    private Specification<Opportunity> capturedSpecification(Pageable pageable) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Opportunity>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(captor.capture(), eq(pageable));
        return captor.getValue();
    }

    /**
     * Runs the specification against stubbed criteria objects and hands back the
     * root so the test can see which attributes the filters actually touched.
     */
    @SuppressWarnings("unchecked")
    private Root<Opportunity> evaluate(Specification<Opportunity> specification) {
        Root<Opportunity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<String> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Join<Opportunity, Tag> tagJoin = mock(Join.class);

        lenient().doReturn(path).when(root).get(anyString());
        lenient().doReturn(path).when(path).get(anyString());
        lenient().doReturn(tagJoin).when(root).join(anyString());
        lenient().doReturn(path).when(tagJoin).get(anyString());

        lenient().when(builder.equal(ArgumentMatchers.<Expression<?>>any(), ArgumentMatchers.<Object>any()))
                .thenReturn(predicate);
        lenient().when(builder.lower(ArgumentMatchers.<Expression<String>>any())).thenReturn(path);
        lenient().when(builder.like(ArgumentMatchers.<Expression<String>>any(), anyString(), anyChar()))
                .thenReturn(predicate);
        lenient().when(builder.and(any(), any())).thenReturn(predicate);
        lenient().when(builder.or(any(), any())).thenReturn(predicate);
        lenient().when(builder.isNull(ArgumentMatchers.<Expression<?>>any())).thenReturn(predicate);
        lenient().doReturn(predicate).when(builder).greaterThanOrEqualTo(
                ArgumentMatchers.<Expression<LocalDateTime>>any(), ArgumentMatchers.<LocalDateTime>any());

        specification.toPredicate(root, query, builder);
        return root;
    }

    private static Opportunity opportunity(Long id, OpportunityStatus status) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(id);
        opportunity.setTitle("Hackathon");
        opportunity.setOrganization("Acme");
        opportunity.setType("HACKATHON");
        opportunity.setStatus(status);
        return opportunity;
    }

    private static Tag tag(Long id, String name) {
        Tag tag = new Tag(name);
        tag.setId(id);
        return tag;
    }
}
