package com.cosmocats.cosmo_cats.repository;

import com.cosmocats.cosmo_cats.TestcontainersConfiguration;
import com.cosmocats.cosmo_cats.repository.entity.CategoryEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CategoryRepositoryIT {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private EntityManager entityManager;

    private CategoryEntity newCategory(String name) {
        CategoryEntity entity = new CategoryEntity();
        entity.setPublicId(UUID.randomUUID());
        entity.setName(name);
        return entity;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // категорії Yarn і Milk створює міграція 003, тобто тест підтверджує, що Liquibase спрацював
    @Test
    void shouldFindCategoryCreatedByMigration() {
        UUID yarnId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

        CategoryEntity yarn = categoryRepository.findByPublicId(yarnId).orElseThrow();

        assertThat(yarn.getName()).isEqualTo("Yarn");
    }

    @Test
    void shouldSaveAndFindCategory() {
        CategoryEntity saved = categoryRepository.save(newCategory("Stars"));
        flushAndClear();

        CategoryEntity found = categoryRepository.findByPublicId(saved.getPublicId()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Stars");
    }

    @Test
    void shouldUpdateCategory() {
        CategoryEntity saved = categoryRepository.save(newCategory("Stars"));
        flushAndClear();

        CategoryEntity found = categoryRepository.findByPublicId(saved.getPublicId()).orElseThrow();
        found.setName("Comets");
        flushAndClear();

        assertThat(categoryRepository.findByPublicId(saved.getPublicId()).orElseThrow().getName())
                .isEqualTo("Comets");
    }

    @Test
    void shouldDeleteCategory() {
        CategoryEntity saved = categoryRepository.save(newCategory("Stars"));
        flushAndClear();

        categoryRepository.deleteById(saved.getId());
        flushAndClear();

        assertThat(categoryRepository.findByPublicId(saved.getPublicId())).isEmpty();
    }

    @Test
    void shouldRejectDuplicateCategoryName() {
        categoryRepository.saveAndFlush(newCategory("Stars"));

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(newCategory("Stars")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
