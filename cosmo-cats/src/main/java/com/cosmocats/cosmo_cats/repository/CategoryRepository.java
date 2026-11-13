package com.cosmocats.cosmo_cats.repository;

import com.cosmocats.cosmo_cats.repository.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findByPublicId(UUID publicId);
}