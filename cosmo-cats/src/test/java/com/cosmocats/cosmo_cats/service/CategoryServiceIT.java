package com.cosmocats.cosmo_cats.service;

import com.cosmocats.cosmo_cats.TestcontainersConfiguration;
import com.cosmocats.cosmo_cats.domain.Category;
import com.cosmocats.cosmo_cats.exception.CategoryNotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, CategoryService.class})
class CategoryServiceIT {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private EntityManager entityManager;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldCreateAndFindCategory() {
        Category created = categoryService.create(new Category(UUID.randomUUID(), "Stars"));
        flushAndClear();

        assertThat(categoryService.findById(created.id()).name()).isEqualTo("Stars");
    }

    @Test
    void shouldThrowWhenCategoryNotFound() {
        assertThatThrownBy(() -> categoryService.findById(UUID.randomUUID()))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    // Yarn і Milk створила міграція 003
    @Test
    void shouldReturnAllCategories() {
        assertThat(categoryService.findAll()).extracting(Category::name).contains("Yarn", "Milk");
    }

    @Test
    void shouldDeleteCategory() {
        Category created = categoryService.create(new Category(UUID.randomUUID(), "Stars"));
        flushAndClear();

        categoryService.delete(created.id());
        flushAndClear();

        assertThatThrownBy(() -> categoryService.findById(created.id()))
                .isInstanceOf(CategoryNotFoundException.class);
    }
}
