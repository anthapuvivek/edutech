package com.learntrix.edtech.common.pagination;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class PageResponse<T> {
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;
    private final int totalPages;

    /**
     * Note the page number is 1-based here while Spring's Page is 0-based. This DTO is the
     * client contract (src/types/index.ts Paginated), and the client counts from 1.
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .items(page.getContent())
                .page(page.getNumber() + 1)
                .pageSize(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
