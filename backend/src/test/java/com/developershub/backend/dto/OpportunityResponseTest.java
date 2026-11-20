package com.developershub.backend.dto;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityLevel;
import com.developershub.backend.entity.OpportunityStatus;
import com.developershub.backend.entity.OpportunityType;
import com.developershub.backend.entity.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpportunityResponseTest {

    @Test
    void mapsEveryFieldFromTheEntity() {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(42L);
        opportunity.setTitle("Global AI Hackathon");
        opportunity.setOrganization("DevsHub");
        opportunity.setType(OpportunityType.HACKATHON.name());
        opportunity.setLevel(OpportunityLevel.INTERMEDIATE);
        opportunity.setDescription("A weekend of building");
        opportunity.setUrl("https://example.dev/hackathon");
        opportunity.setDeadline(LocalDate.now().plusDays(5).atTime(18, 0));
        opportunity.setStatus(OpportunityStatus.APPROVED);
        opportunity.setTags(Set.of(tag(7L, "java"), tag(8L, "spring")));
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1).withNano(0);
        ReflectionTestUtils.setField(opportunity, "createdAt", createdAt);

        OpportunityResponse response = OpportunityResponse.from(opportunity);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.title()).isEqualTo("Global AI Hackathon");
        assertThat(response.organization()).isEqualTo("DevsHub");
        assertThat(response.type()).isEqualTo("HACKATHON");
        assertThat(response.level()).isEqualTo(OpportunityLevel.INTERMEDIATE);
        assertThat(response.description()).isEqualTo("A weekend of building");
        assertThat(response.url()).isEqualTo("https://example.dev/hackathon");
        assertThat(response.deadline()).isEqualTo(LocalDate.now().plusDays(5).atTime(18, 0));
        assertThat(response.status()).isEqualTo(OpportunityStatus.APPROVED);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.daysUntilDeadline()).isEqualTo(5L);
        assertThat(response.tags()).containsExactlyInAnyOrder(
                new OpportunityResponse.TagResponse(7L, "java"),
                new OpportunityResponse.TagResponse(8L, "spring"));
    }

    @Test
    void daysUntilDeadlineIsNullWhenTheDeadlineIsNull() {
        Opportunity opportunity = new Opportunity();
        opportunity.setTitle("No deadline");
        opportunity.setOrganization("DevsHub");
        opportunity.setType(OpportunityType.PROGRAM.name());

        OpportunityResponse response = OpportunityResponse.from(opportunity);

        assertThat(response.deadline()).isNull();
        assertThat(response.daysUntilDeadline()).isNull();
    }

    @Test
    void computesDaysUntilDeadlineFromToday() {
        Opportunity opportunity = new Opportunity();
        // Late in the day so a clock ticking past midnight cannot shift the count.
        opportunity.setDeadline(LocalDate.now().plusDays(10).atTime(23, 59));

        OpportunityResponse response = OpportunityResponse.from(opportunity);

        assertThat(response.daysUntilDeadline()).isEqualTo(10L);
    }

    @Test
    void daysUntilDeadlineIsNegativeForADeadlineInThePast() {
        Opportunity opportunity = new Opportunity();
        opportunity.setDeadline(LocalDate.now().minusDays(3).atTime(12, 0));

        OpportunityResponse response = OpportunityResponse.from(opportunity);

        assertThat(response.daysUntilDeadline()).isEqualTo(-3L);
    }

    @Test
    void daysUntilDeadlineIsZeroForADeadlineLaterToday() {
        Opportunity opportunity = new Opportunity();
        opportunity.setDeadline(LocalDate.now().atTime(0, 0));

        OpportunityResponse response = OpportunityResponse.from(opportunity);

        assertThat(response.daysUntilDeadline()).isZero();
    }

    @Test
    void mapsTagsToTagResponsesWithIdAndName() {
        Opportunity opportunity = new Opportunity();
        opportunity.setTags(Set.of(tag(7L, "java")));

        OpportunityResponse response = OpportunityResponse.from(opportunity);

        assertThat(response.tags()).containsExactly(new OpportunityResponse.TagResponse(7L, "java"));
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag(name);
        tag.setId(id);
        return tag;
    }
}
