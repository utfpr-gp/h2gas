package com.h2gas.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtil {

    public static Pageable getPageableRequest(Integer page, Integer size) {
        int pages = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size < 1) ? 10 : size;
        int maxSize = 50;

        if (pageSize > maxSize) {
            pageSize = maxSize;
        }

        return PageRequest.of(pages, pageSize);
    }

    public static Pageable getPageableRequest(Integer page, Integer size, String sort) {
        Pageable base = getPageableRequest(page, size);
        if (sort == null || sort.isBlank()) {
            return base;
        }

        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1
                ? Sort.Direction.fromString(parts[1])
                : Sort.Direction.ASC;
        return PageRequest.of(base.getPageNumber(), base.getPageSize(), Sort.by(direction, parts[0]));
    }
}
