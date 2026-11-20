package com.developershub.backend.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void mapsContentAndCarriesPaginationMetadata() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        PageResponse<String> response = PageResponse.of(page, String::toUpperCase);

        assertThat(response.content()).containsExactly("A", "B");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void marksTheOnlyPageAsFirstAndLast() {
        Page<String> page = new PageImpl<>(List.of("only"), PageRequest.of(0, 10), 1);

        PageResponse<String> response = PageResponse.of(page, value -> value);

        assertThat(response.content()).containsExactly("only");
        assertThat(response.page()).isZero();
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    void marksTheLastPageAsLast() {
        Page<String> page = new PageImpl<>(List.of("e"), PageRequest.of(2, 2), 5);

        PageResponse<String> response = PageResponse.of(page, value -> value);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
    }

    @Test
    void mapsAnEmptyPageToAnEmptyContentList() {
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        PageResponse<String> response = PageResponse.of(page, value -> value);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }
}
