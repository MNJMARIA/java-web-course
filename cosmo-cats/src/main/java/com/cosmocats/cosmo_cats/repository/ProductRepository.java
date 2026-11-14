package com.cosmocats.cosmo_cats.repository;

import com.cosmocats.cosmo_cats.repository.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    // @EntityGraph підвантажує категорію одним запитом разом із продуктом (щоб не було N+1)
    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findByPublicId(UUID publicId);

    // чи є вже продукт з такою назвою (без урахування регістру) у цій категорії
    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findByCategoryPublicIdAndNameIgnoreCase(UUID categoryPublicId, String name);

    // список із пагінацією; категорії підвантажуються разом зі сторінкою
    @Override
    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findAll(Pageable pageable);

    void deleteByPublicId(UUID publicId);
}