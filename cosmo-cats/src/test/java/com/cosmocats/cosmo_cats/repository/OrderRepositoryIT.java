package com.cosmocats.cosmo_cats.repository;

import com.cosmocats.cosmo_cats.TestcontainersConfiguration;
import com.cosmocats.cosmo_cats.domain.OrderStatus;
import com.cosmocats.cosmo_cats.repository.entity.CategoryEntity;
import com.cosmocats.cosmo_cats.repository.entity.OrderEntity;
import com.cosmocats.cosmo_cats.repository.entity.OrderItemEntity;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OrderRepositoryIT {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private EntityManager entityManager;

    private ProductEntity yarn;
    private ProductEntity milk;

    // замовлення посилається на продукти, тому спершу створюємо категорію і два продукти
    @BeforeEach
    void setUp() {
        CategoryEntity category = new CategoryEntity();
        category.setPublicId(UUID.randomUUID());
        category.setName("Order test category");
        categoryRepository.save(category);
        yarn = productRepository.save(newProduct("Galaxy yarn", category));
        milk = productRepository.save(newProduct("Comet milk", category));
    }

    private ProductEntity newProduct(String name, CategoryEntity category) {
        ProductEntity product = new ProductEntity();
        product.setPublicId(UUID.randomUUID());
        product.setName(name);
        product.setPrice(new BigDecimal("19.99"));
        product.setCategory(category);
        return product;
    }

    // створює замовлення з однією позицією (кількість 2) на кожен переданий продукт
    private OrderEntity newOrder(ProductEntity... products) {
        OrderEntity order = new OrderEntity();
        order.setPublicId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        for (ProductEntity product : products) {
            OrderItemEntity item = new OrderItemEntity();
            item.setProduct(product);
            item.setQuantity(2);
            order.addItem(item);
        }
        return order;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldSaveOrderWithItems() {
        OrderEntity saved = orderRepository.save(newOrder(yarn, milk));
        flushAndClear();

        OrderEntity found = orderRepository.findByPublicId(saved.getPublicId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(found.getItems()).hasSize(2);
    }

    @Test
    void shouldUpdateItemQuantity() {
        OrderEntity saved = orderRepository.save(newOrder(yarn));
        flushAndClear();

        OrderEntity found = orderRepository.findByPublicId(saved.getPublicId()).orElseThrow();
        found.getItems().get(0).setQuantity(5);
        flushAndClear();

        OrderEntity updated = orderRepository.findByPublicId(saved.getPublicId()).orElseThrow();
        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    // orphanRemoval: позиція, прибрана зі списку, видаляється з бази
    @Test
    void shouldDeleteRemovedItem() {
        OrderEntity saved = orderRepository.save(newOrder(yarn, milk));
        flushAndClear();

        OrderEntity found = orderRepository.findByPublicId(saved.getPublicId()).orElseThrow();
        found.getItems().remove(0);
        flushAndClear();

        assertThat(orderRepository.findByPublicId(saved.getPublicId()).orElseThrow().getItems()).hasSize(1);
    }

    // cascade: разом із замовленням видаляються його позиції
    @Test
    void shouldDeleteOrderWithItems() {
        OrderEntity saved = orderRepository.save(newOrder(yarn, milk));
        flushAndClear();

        orderRepository.delete(orderRepository.findByPublicId(saved.getPublicId()).orElseThrow());
        flushAndClear();

        Long itemsLeft = entityManager
                .createQuery("select count(i) from OrderItemEntity i", Long.class)
                .getSingleResult();
        assertThat(orderRepository.findByPublicId(saved.getPublicId())).isEmpty();
        assertThat(itemsLeft).isZero();
    }
}