package com.cosmocats.cosmo_cats.repository.entity;

import com.cosmocats.cosmo_cats.domain.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")   // "order" у SQL зарезервоване слово
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_id_seq", allocationSize = 50)
    private Long id;

    @NaturalId
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // при збереженні замовлення зберігаються й позиції; видалена позиція видаляється з бази
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    // зручний метод: додає позицію і виставляє їй зворотне посилання на замовлення
    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }
}