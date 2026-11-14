package com.cosmocats.cosmo_cats.service;

import com.cosmocats.cosmo_cats.TestcontainersConfiguration;
import com.cosmocats.cosmo_cats.domain.CartItem;
import com.cosmocats.cosmo_cats.domain.Order;
import com.cosmocats.cosmo_cats.domain.OrderStatus;
import com.cosmocats.cosmo_cats.exception.OrderNotFoundException;
import com.cosmocats.cosmo_cats.exception.ProductNotFoundException;
import com.cosmocats.cosmo_cats.repository.CategoryRepository;
import com.cosmocats.cosmo_cats.repository.ProductRepository;
import com.cosmocats.cosmo_cats.repository.entity.CategoryEntity;
import com.cosmocats.cosmo_cats.repository.entity.ProductEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, OrderService.class})
class OrderServiceIT {

    @Autowired
    private OrderService orderService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private EntityManager entityManager;

    private UUID productId;

    @BeforeEach
    void setUp() {
        CategoryEntity category = new CategoryEntity();
        category.setPublicId(UUID.randomUUID());
        category.setName("Order service category");
        categoryRepository.save(category);

        ProductEntity product = new ProductEntity();
        product.setPublicId(UUID.randomUUID());
        product.setName("Galaxy yarn");
        product.setPrice(new BigDecimal("19.99"));
        product.setCategory(category);
        productRepository.save(product);
        productId = product.getPublicId();
    }

    private Order newOrder(UUID itemProductId) {
        return new Order(UUID.randomUUID(), List.of(new CartItem(itemProductId, 2)),
                OrderStatus.PENDING, Instant.now());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldCreateAndFindOrder() {
        Order created = orderService.create(newOrder(productId));
        flushAndClear();

        Order found = orderService.findById(created.id());

        assertThat(found.items()).hasSize(1);
        assertThat(found.items().get(0).productId()).isEqualTo(productId);
        assertThat(found.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void shouldThrowWhenOrderContainsUnknownProduct() {
        assertThatThrownBy(() -> orderService.create(newOrder(UUID.randomUUID())))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldUpdateOrderStatus() {
        Order created = orderService.create(newOrder(productId));
        flushAndClear();
        // будь-який статус, відмінний від PENDING
        OrderStatus other = Arrays.stream(OrderStatus.values())
                .filter(status -> status != OrderStatus.PENDING).findFirst().orElseThrow();

        orderService.updateStatus(created.id(), other);
        flushAndClear();

        assertThat(orderService.findById(created.id()).status()).isEqualTo(other);
    }

    @Test
    void shouldDeleteOrder() {
        Order created = orderService.create(newOrder(productId));
        flushAndClear();

        orderService.delete(created.id());
        flushAndClear();

        assertThatThrownBy(() -> orderService.findById(created.id()))
                .isInstanceOf(OrderNotFoundException.class);
    }
}