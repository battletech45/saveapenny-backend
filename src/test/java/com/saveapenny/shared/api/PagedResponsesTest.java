package com.saveapenny.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PagedResponsesTest {

    @Test
    void from_whenPageHasNextAndPrevious_mapsAllFields() {
        PageImpl<String> page = new PageImpl<>(
                List.of("b", "c"),
                PageRequest.of(1, 2),
                5);

        PagedResponse<String> response = PagedResponses.from(page);

        assertThat(response.items()).containsExactly("b", "c");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalItems()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isTrue();
    }

    @Test
    void from_whenPageIsEmpty_keepsItemsAsEmptyList() {
        PageImpl<String> page = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0);

        PagedResponse<String> response = PagedResponses.from(page);

        assertThat(response.items()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalItems()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }
}
