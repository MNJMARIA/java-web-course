package com.cosmocats.cosmo_cats.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        UUID categoryId
) {
}
