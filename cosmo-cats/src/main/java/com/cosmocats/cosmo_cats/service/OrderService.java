package com.cosmocats.cosmo_cats.service;

import com.cosmocats.cosmo_cats.domain.CartItem;
import com.cosmocats.cosmo_cats.domain.Order;
import com.cosmocats.cosmo_cats.domain.OrderStatus;
import com.cosmocats.cosmo_cats.exception.OrderNotFoundException;
import com.cosmocats.cosmo_cats.exception.ProductNotFoundException;
import com.cosmocats.cosmo_cats.repository.OrderRepository;
import com.cosmocats.cosmo_cats.repository.ProductRepository;
import com.cosmocats.cosmo_cats.repository.entity.OrderEntity;
import com.cosmocats.cosmo_cats.repository.entity.OrderItemEntity;
import com.cosmocats.cosmo_cats.repository.entity.ProductEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order create(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setPublicId(order.id());
        entity.setStatus(order.status());
        entity.setCreatedAt(order.createdAt());

        for (CartItem item : order.items()) {
            ProductEntity product = productRepository.findByPublicId(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setProduct(product);
            itemEntity.setQuantity(item.quantity());
            entity.addItem(itemEntity);
        }
        return toDomain(orderRepository.save(entity));
    }

    public Order findById(UUID id) {
        return orderRepository.findByPublicId(id)
                .map(this::toDomain)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    // зміни сутності всередині транзакції зберігаються автоматично при її завершенні
    @Transactional
    public Order updateStatus(UUID id, OrderStatus status) {
        OrderEntity entity = orderRepository.findByPublicId(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        entity.setStatus(status);
        return toDomain(entity);
    }

    @Transactional
    public void delete(UUID id) {
        orderRepository.findByPublicId(id).ifPresent(orderRepository::delete);
    }

    private Order toDomain(OrderEntity entity) {
        List<CartItem> items = entity.getItems().stream()
                .map(item -> new CartItem(item.getProduct().getPublicId(), item.getQuantity()))
                .toList();
        return new Order(entity.getPublicId(), items, entity.getStatus(), entity.getCreatedAt());
    }
}
