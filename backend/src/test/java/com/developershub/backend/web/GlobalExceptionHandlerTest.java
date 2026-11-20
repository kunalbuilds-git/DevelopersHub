package com.developershub.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsResponseStatusExceptionToItsStatusAndReason() {
        ResponseEntity<ProblemDetail> response =
                handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "No such user"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getDetail()).isEqualTo("No such user");
    }

    @Test
    void fallsBackToTheStatusCodeTextWhenTheReasonIsMissing() {
        ResponseEntity<ProblemDetail> response =
                handler.handleResponseStatus(new ResponseStatusException(HttpStatus.CONFLICT));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getDetail()).isEqualTo("409 CONFLICT");
    }

    @Test
    void mapsValidationFailuresToAnErrorsMapKeyedByField() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new LoginForm(), "loginForm");
        bindingResult.addError(new FieldError("loginForm", "email", "must not be blank"));
        bindingResult.addError(new FieldError("loginForm", "password", "size must be between 8 and 255"));
        Method method = LoginForm.class.getMethod("accept", String.class, String.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getDetail()).isEqualTo("Validation failed");
        assertThat(errorsOf(body)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "email", "must not be blank",
                "password", "size must be between 8 and 255"));
    }

    @Test
    void mapsUnreadableRequestBodiesToBadRequest() {
        ResponseEntity<ProblemDetail> response =
                handler.handleUnreadable(new HttpMessageNotReadableException("broken json", (HttpInputMessage) null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getDetail()).isEqualTo("Malformed request body");
    }

    @Test
    void mapsTypeMismatchesToBadRequestAndNamesTheParameter() throws NoSuchMethodException {
        Method method = Lookup.class.getMethod("find", Long.class);
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "not-a-number", Long.class, "id", new MethodParameter(method, 0), new IllegalArgumentException("bad"));

        ResponseEntity<ProblemDetail> response = handler.handleTypeMismatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getDetail()).isEqualTo("Invalid value for parameter: id");
    }

    @Test
    void mapsDataIntegrityViolationsToConflict() {
        ResponseEntity<ProblemDetail> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate key"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getDetail()).isEqualTo("The request conflicts with an existing resource");
    }

    @Test
    void mapsAccessDeniedToForbidden() {
        ResponseEntity<ProblemDetail> response =
                handler.handleAccessDenied(new AccessDeniedException("missing role"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(403);
        assertThat(body.getDetail()).isEqualTo("You do not have permission to perform this action");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> errorsOf(ProblemDetail body) {
        return (Map<String, String>) body.getProperties().get("errors");
    }

    private static class LoginForm {
        public void accept(String email, String password) {
        }
    }

    private static class Lookup {
        public void find(Long id) {
        }
    }
}
