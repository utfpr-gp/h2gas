package com.h2gas.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
}
