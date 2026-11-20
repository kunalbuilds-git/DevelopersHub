package com.developershub.backend.dto;

import com.developershub.backend.entity.Opportunity;
import com.developershub.backend.entity.OpportunityLevel;
import com.developershub.backend.entity.OpportunityType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void rejectsBlankEmail() {
        Set<ConstraintViolation<AuthRequest>> violations = validator.validate(authRequest("   ", "Password123!"));

        assertThat(violations).isNotEmpty();
        assertThat(propertiesOf(violations)).contains("email");
    }

    @Test
    void rejectsInvalidEmail() {
        Set<ConstraintViolation<AuthRequest>> violations = validator.validate(authRequest("not-an-email", "Password123!"));

        assertThat(violations).isNotEmpty();
        assertThat(propertiesOf(violations)).contains("email");
    }

    @Test
    void acceptsValidCredentials() {
        assertThat(validator.validate(authRequest("student@example.com", "Password123!"))).isEmpty();
    }

    @Test
    void rejectsPasswordShorterThan8Characters() {
        Set<ConstraintViolation<AuthRequest>> violations = validator.validate(authRequest("student@example.com", "short"));

        assertThat(violations).hasSize(1);
        ConstraintViolation<AuthRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
        assertThat(violation.getMessage()).isEqualTo("Password must be between 8 and 255 characters");
    }

    @Test
    void rejectsPasswordLongerThan255Characters() {
        Set<ConstraintViolation<AuthRequest>> violations =
                validator.validate(authRequest("student@example.com", "x".repeat(256)));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Password must be between 8 and 255 characters");
    }

    @Test
    void rejectsEmailLongerThan255Characters() {
        Set<ConstraintViolation<AuthRequest>> violations =
                validator.validate(authRequest("a".repeat(250) + "@example.com", "Password123!"));

        assertThat(violations).isNotEmpty();
        assertThat(propertiesOf(violations)).contains("email");
        assertThat(violations).anyMatch(violation -> violation.getMessage().contains("255"));
    }

    @Test
    void rejectsBlankRefreshToken() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("   ");

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void rejectsBlankBootstrapEmail() {
        BootstrapRequest request = new BootstrapRequest();
        request.setEmail("   ");
        request.setSetupToken("setup-token");

        assertThat(validator.validate(request)).isNotEmpty();
        assertThat(propertiesOf(validator.validate(request))).contains("email");
    }

    @Test
    void rejectsInvalidBootstrapEmail() {
        BootstrapRequest request = new BootstrapRequest();
        request.setEmail("not-an-email");
        request.setSetupToken("setup-token");

        assertThat(validator.validate(request)).isNotEmpty();
        assertThat(propertiesOf(validator.validate(request))).contains("email");
    }

    @Test
    void rejectsBlankSetupToken() {
        BootstrapRequest request = new BootstrapRequest();
        request.setEmail("admin@example.com");
        request.setSetupToken("  ");

        assertThat(validator.validate(request)).isNotEmpty();
        assertThat(propertiesOf(validator.validate(request))).contains("setupToken");
    }

    @Test
    void acceptsAValidBootstrapRequest() {
        BootstrapRequest request = new BootstrapRequest();
        request.setEmail("admin@example.com");
        request.setSetupToken("setup-token");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankOpportunityTitle() {
        OpportunityRequest request = validOpportunityRequest();
        request.setTitle("   ");

        assertThat(propertiesOf(validator.validate(request))).contains("title");
    }

    @Test
    void rejectsBlankOpportunityOrganization() {
        OpportunityRequest request = validOpportunityRequest();
        request.setOrganization("");

        assertThat(propertiesOf(validator.validate(request))).contains("organization");
    }

    @Test
    void rejectsNullOpportunityType() {
        OpportunityRequest request = validOpportunityRequest();
        request.setType(null);

        assertThat(propertiesOf(validator.validate(request))).contains("type");
    }

    @Test
    void rejectsTitleLongerThan255Characters() {
        OpportunityRequest request = validOpportunityRequest();
        request.setTitle("x".repeat(256));

        assertThat(propertiesOf(validator.validate(request))).contains("title");
    }

    @Test
    void rejectsBlankTagInsideTheTagSet() {
        OpportunityRequest request = validOpportunityRequest();
        request.setTags(Set.of("java", " "));

        Set<ConstraintViolation<OpportunityRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(propertiesOf(violations)).anyMatch(property -> property.startsWith("tags"));
    }

    @Test
    void acceptsAValidOpportunityRequest() {
        assertThat(validator.validate(validOpportunityRequest())).isEmpty();
    }

    @Test
    void toOpportunityMapsTypeToItsNameAndDefaultsLevelToAny() {
        OpportunityRequest request = validOpportunityRequest();
        request.setType(OpportunityType.INTERNSHIP);
        request.setLevel(null);

        Opportunity opportunity = request.toOpportunity();

        assertThat(opportunity.getType()).isInstanceOf(String.class).isEqualTo("INTERNSHIP");
        assertThat(opportunity.getLevel()).isEqualTo(OpportunityLevel.ANY);
    }

    @Test
    void toOpportunityKeepsTheExplicitLevel() {
        OpportunityRequest request = validOpportunityRequest();
        request.setLevel(OpportunityLevel.ADVANCED);

        Opportunity opportunity = request.toOpportunity();

        assertThat(opportunity.getLevel()).isEqualTo(OpportunityLevel.ADVANCED);
    }

    private AuthRequest authRequest(String email, String password) {
        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private OpportunityRequest validOpportunityRequest() {
        OpportunityRequest request = new OpportunityRequest();
        request.setTitle("Global AI Hackathon");
        request.setOrganization("DevsHub");
        request.setType(OpportunityType.HACKATHON);
        request.setLevel(OpportunityLevel.BEGINNER);
        request.setTags(Set.of("java"));
        return request;
    }

    private Set<String> propertiesOf(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
