package com.cosmocats.cosmo_cats.repository;

import com.cosmocats.cosmo_cats.TestcontainersConfiguration;
import com.cosmocats.cosmo_cats.repository.entity.CategoryEntity;
import com.cosmocats.cosmo_cats.repository.entity.ProductEntity;
import jakarta.persistence.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // не підміняти PostgreSQL на вбудовану базу
@Import(TestcontainersConfiguration.class)
class ProductRepositoryIT {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private EntityManager entityManager;

    private CategoryEntity category;

    @BeforeEach
    void setUp() {
        category = saveCategory("Test category");
    }

    private CategoryEntity saveCategory(String name) {
        CategoryEntity entity = new CategoryEntity();
        entity.setPublicId(UUID.randomUUID());
        entity.setName(name);
        return categoryRepository.save(entity);
    }

    private ProductEntity newProduct(String name, CategoryEntity productCategory) {
        ProductEntity product = new ProductEntity();
        product.setPublicId(UUID.randomUUID());
        product.setName(name);
        product.setDescription("test");
        product.setPrice(new BigDecimal("19.99"));
        product.setCategory(productCategory);
        return product;
    }

    // відправляє зміни в базу і очищає кеш Hibernate, щоб наступне читання було справжнім SELECT
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldSaveAndFindProduct() {
        ProductEntity saved = productRepository.save(newProduct("Galaxy yarn", category));
        flushAndClear();

        ProductEntity found = productRepository.findByPublicId(saved.getPublicId()).orElseThrow();

        assertThat(found.getId()).isNotNull(); // id видала sequence
        assertThat(found.getName()).isEqualTo("Galaxy yarn");
        assertThat(found.getPrice()).isEqualByComparingTo("19.99");
        assertThat(found.getCategory().getPublicId()).isEqualTo(category.getPublicId());
    }

    @Test
    void shouldFindByCategoryAndNameIgnoringCase() {
        productRepository.save(newProduct("Galaxy yarn", category));
        flushAndClear();

        assertThat(productRepository
                .findByCategoryPublicIdAndNameIgnoreCase(category.getPublicId(), "GALAXY YARN")).isPresent();
        assertThat(productRepository
                .findByCategoryPublicIdAndNameIgnoreCase(UUID.randomUUID(), "Galaxy yarn")).isEmpty();
    }

    @Test
    void shouldReturnPageSortedByName() {
        productRepository.save(newProduct("Nova", category));
        productRepository.save(newProduct("Aster", category));
        productRepository.save(newProduct("Comet", category));
        flushAndClear();

        Page<ProductEntity> page = productRepository.findAll(PageRequest.of(0, 2, Sort.by("name")));

        assertThat(page.getContent()).extracting(ProductEntity::getName).containsExactly("Aster", "Comet");
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void shouldUpdateProduct() {
        ProductEntity saved = productRepository.save(newProduct("Galaxy yarn", category));
        flushAndClear();

        ProductEntity found = productRepository.findByPublicId(saved.getPublicId()).orElseThrow();
        found.setPrice(new BigDecimal("25.50"));
        flushAndClear();

        ProductEntity updated = productRepository.findByPublicId(saved.getPublicId()).orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo("25.50");
    }

    @Test
    void shouldDeleteProduct() {
        ProductEntity saved = productRepository.save(newProduct("Galaxy yarn", category));
        flushAndClear();

        productRepository.deleteByPublicId(saved.getPublicId());
        flushAndClear();

        assertThat(productRepository.findByPublicId(saved.getPublicId())).isEmpty();
    }

    // перевіряє unique constraint (name, category_id) з міграції 002
    @Test
    void shouldRejectSameNameInSameCategory() {
        productRepository.saveAndFlush(newProduct("Galaxy yarn", category));

        assertThatThrownBy(() -> productRepository.saveAndFlush(newProduct("Galaxy yarn", category)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameNameInAnotherCategory() {
        CategoryEntity other = saveCategory("Other category");
        productRepository.saveAndFlush(newProduct("Galaxy yarn", category));

        productRepository.saveAndFlush(newProduct("Galaxy yarn", other));

        assertThat(productRepository.findAll()).hasSize(2);
    }
}