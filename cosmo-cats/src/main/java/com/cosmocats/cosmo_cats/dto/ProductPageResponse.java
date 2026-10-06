package com.cosmocats.cosmo_cats.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductPageResponse(
        List<ProductResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
