package com.cosmocats.cosmo_cats.repository;

import com.cosmocats.cosmo_cats.repository.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<OrderEntity> findByPublicId(UUID publicId);
}