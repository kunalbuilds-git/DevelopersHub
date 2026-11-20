package com.developershub.backend.specification;

import com.developershub.backend.entity.Opportunity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpportunitySpecificationsTest {

    @Test
    void nullAndBlankFiltersSkipFilteringEntirely() {
        // These short-circuit before the criteria API is touched, so nulls are safe.
        assertThat(OpportunitySpecifications.hasType(null).toPredicate(null, null, null)).isNull();
        assertThat(OpportunitySpecifications.hasLevel(null).toPredicate(null, null, null)).isNull();
        assertThat(OpportunitySpecifications.hasStatus(null).toPredicate(null, null, null)).isNull();
        assertThat(OpportunitySpecifications.hasSubmittedBy(null).toPredicate(null, null, null)).isNull();
        assertThat(OpportunitySpecifications.hasTag(null).toPredicate(null, null, null)).isNull();
        assertThat(OpportunitySpecifications.hasTag("  ").toPredicate(null, null, null)).isNull();
        assertThat(OpportunitySpecifications.hasKeyword(null).toPredicate(null, null, null)).isNull();
        assertThat(OpportunitySpecifications.hasKeyword("  ").toPredicate(null, null, null)).isNull();
    }

    @Test
    void escapesPercentWildcardsInTheKeywordPattern() {
        KeywordFixture fixture = new KeywordFixture();

        Predicate result = OpportunitySpecifications.hasKeyword("100%").toPredicate(fixture.root, null, fixture.cb);

        assertThat(fixture.capturedPatterns()).containsOnly("%100\\%%");
        assertThat(result).isSameAs(fixture.or);
    }

    @Test
    void escapesUnderscoreWildcardsInTheKeywordPattern() {
        KeywordFixture fixture = new KeywordFixture();

        OpportunitySpecifications.hasKeyword("a_b").toPredicate(fixture.root, null, fixture.cb);

        assertThat(fixture.capturedPatterns()).containsOnly("%a\\_b%");
    }

    @Test
    void escapesBackslashesBeforeOtherWildcardsInTheKeywordPattern() {
        KeywordFixture fixture = new KeywordFixture();

        OpportunitySpecifications.hasKeyword("a\\b%c").toPredicate(fixture.root, null, fixture.cb);

        assertThat(fixture.capturedPatterns()).containsOnly("%a\\\\b\\%c%");
    }

    @Test
    void lowercasesTheKeywordBeforeWrappingItInWildcards() {
        KeywordFixture fixture = new KeywordFixture();

        OpportunitySpecifications.hasKeyword("Java").toPredicate(fixture.root, null, fixture.cb);

        assertThat(fixture.capturedPatterns()).containsOnly("%java%");
    }

    @Test
    void hasNotExpiredAllowsMissingDeadlinesAndOnesThatHaveNotPassed() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        @SuppressWarnings("unchecked")
        Root<Opportunity> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        Path<LocalDateTime> deadlinePath = mock(Path.class);
        Predicate isNull = mock(Predicate.class);
        Predicate notExpired = mock(Predicate.class);
        Predicate or = mock(Predicate.class);
        LocalDateTime now = LocalDateTime.now();

        doReturn(deadlinePath).when(root).get("deadline");
        when(cb.isNull(deadlinePath)).thenReturn(isNull);
        when(cb.greaterThanOrEqualTo(deadlinePath, now)).thenReturn(notExpired);
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(or);

        Predicate result = OpportunitySpecifications.hasNotExpired(now).toPredicate(root, null, cb);

        assertThat(result).isSameAs(or);
        verify(cb).isNull(deadlinePath);
        verify(cb).greaterThanOrEqualTo(deadlinePath, now);
    }

    /** Minimal criteria scaffolding for the keyword LIKE assertions. */
    private static final class KeywordFixture {

        private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
        @SuppressWarnings("unchecked")
        private final Root<Opportunity> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        private final Path<String> titlePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        private final Path<String> organizationPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        private final Expression<String> lowerTitle = mock(Expression.class);
        @SuppressWarnings("unchecked")
        private final Expression<String> lowerOrganization = mock(Expression.class);
        private final Predicate titleLike = mock(Predicate.class);
        private final Predicate organizationLike = mock(Predicate.class);
        private final Predicate or = mock(Predicate.class);

        private KeywordFixture() {
            doReturn(titlePath).when(root).get("title");
            doReturn(organizationPath).when(root).get("organization");
            when(cb.lower(titlePath)).thenReturn(lowerTitle);
            when(cb.lower(organizationPath)).thenReturn(lowerOrganization);
            when(cb.like(eq(lowerTitle), anyString(), eq('\\'))).thenReturn(titleLike);
            when(cb.like(eq(lowerOrganization), anyString(), eq('\\'))).thenReturn(organizationLike);
            when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(or);
        }

        private List<String> capturedPatterns() {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(cb, times(2)).like(ArgumentMatchers.<Expression<String>>any(), captor.capture(), eq('\\'));
            return captor.getAllValues();
        }
    }
}
