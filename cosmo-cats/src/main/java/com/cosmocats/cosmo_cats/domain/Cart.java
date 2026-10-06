package com.cosmocats.cosmo_cats.domain;

import java.util.List;
import java.util.UUID;

public record Cart(
        UUID id,
        List<CartItem> items
) {
}
