package com.cosmocats.cosmo_cats.domain;

import java.util.UUID;

public record CartItem(
        UUID productId,
        int quantity
) {
}
