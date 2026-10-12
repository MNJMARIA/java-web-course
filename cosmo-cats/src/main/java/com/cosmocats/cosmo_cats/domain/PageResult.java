package com.cosmocats.cosmo_cats.domain;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages

) {
}
