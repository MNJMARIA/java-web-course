package com.cosmocats.cosmo_cats.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {
    @Test
    void shouldCreateDomainObjects() {
        UUID id = UUID.randomUUID();
        CartItem item = new CartItem(id, 2);

        Cart cart = new Cart(id, List.of(item));
        Order order = new Order(id, List.of(item), OrderStatus.PENDING, Instant.now());
        Category category = new Category(id, "Yarn");

        assertThat(cart.items()).hasSize(1);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(category.name()).isEqualTo("Yarn");
    }
}
