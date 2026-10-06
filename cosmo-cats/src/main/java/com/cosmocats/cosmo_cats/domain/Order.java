package com.cosmocats.cosmo_cats.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        List<CartItem> items,
        OrderStatus status,
        Instant createdAt
) {
}
