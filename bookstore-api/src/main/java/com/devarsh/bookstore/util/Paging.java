package com.devarsh.bookstore.util;

import com.devarsh.bookstore.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paging {
    private Paging() {
    }

    /** Validates page/size and builds a Pageable. */
    public static Pageable of(int page, int size, Sort sort) {
        if (page < 0) {
            throw new BadRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("size must be between 1 and 100");
        }
        return PageRequest.of(page, size, sort);
    }
}
