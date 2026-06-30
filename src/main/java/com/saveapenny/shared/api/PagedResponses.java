package com.saveapenny.shared.api;

import org.springframework.data.domain.Page;

public final class PagedResponses {

    private PagedResponses() {
    }

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }
}
