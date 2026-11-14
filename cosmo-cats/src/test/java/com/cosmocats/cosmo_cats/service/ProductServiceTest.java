package com.cosmocats.cosmo_cats.service;

import com.cosmocats.cosmo_cats.domain.PageResult;
import com.cosmocats.cosmo_cats.domain.Product;
import com.cosmocats.cosmo_cats.exception.CategoryNotFoundException;
import com.cosmocats.cosmo_cats.exception.InvalidPageRequestException;
import com.cosmocats.cosmo_cats.exception.ProductAlreadyExistsException;
import com.cosmocats.cosmo_cats.exception.ProductNotFoundException;
import com.cosmocats.cosmo_cats.repository.CategoryRepository;
import com.cosmocats.cosmo_cats.repository.ProductRepository;
import com.cosmocats.cosmo_cats.repository.entity.CategoryEntity;
import com.cosmocats.cosmo_cats.repository.entity.ProductEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private ProductService service;

    private final UUID categoryId = UUID.randomUUID();
    private CategoryEntity categoryEntity;

    @BeforeEach
    void setUp() {
        categoryEntity = new CategoryEntity();
        categoryEntity.setPublicId(categoryId);
        categoryEntity.setName("Yarn");
    }

    private Product product(String name) {
        return new Product(UUID.randomUUID(), name, "test", new BigDecimal("19.99"), categoryId);
    }

    private ProductEntity entityOf(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setPublicId(product.id());
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setPrice(product.price());
        entity.setCategory(categoryEntity);
        return entity;
    }

    // ---------- create ----------

    @Test
    void shouldCreateProduct() {
        Product product = product("Galaxy yarn");
        when(categoryRepository.findByPublicId(categoryId)).thenReturn(Optional.of(categoryEntity));
        when(productRepository.findByCategoryPublicIdAndNameIgnoreCase(categoryId, "Galaxy yarn"))
                .thenReturn(Optional.empty());
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(call -> call.getArgument(0));

        Product created = service.create(product);

        assertThat(created).isEqualTo(product);
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    void shouldThrowConflictWhenNameExistsInCategory() {
        when(categoryRepository.findByPublicId(categoryId)).thenReturn(Optional.of(categoryEntity));
        when(productRepository.findByCategoryPublicIdAndNameIgnoreCase(categoryId, "Galaxy yarn"))
                .thenReturn(Optional.of(entityOf(product("Galaxy yarn"))));   // інший продукт, інший id

        assertThatThrownBy(() -> service.create(product("Galaxy yarn")))
                .isInstanceOf(ProductAlreadyExistsException.class);
        verify(productRepository, never()).save(any(ProductEntity.class));
    }

    @Test
    void shouldThrowWhenCategoryDoesNotExist() {
        when(categoryRepository.findByPublicId(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(product("Galaxy yarn")))
                .isInstanceOf(CategoryNotFoundException.class);
        verifyNoInteractions(productRepository);
    }

    // ---------- findById ----------

    @Test
    void shouldReturnProductById() {
        Product product = product("Galaxy yarn");
        when(productRepository.findByPublicId(product.id())).thenReturn(Optional.of(entityOf(product)));

        assertThat(service.findById(product.id())).isEqualTo(product);
    }

    @Test
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(productRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(unknownId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    // ---------- findPage ----------

    @Test
    void shouldReturnPageOfProducts() {
        Product product = product("Galaxy yarn");
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entityOf(product)), PageRequest.of(0, 20), 1));

        PageResult<Product> result = service.findPage(0, 20);

        assertThat(result.content()).containsExactly(product);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldThrowWhenPageIsNegative() {
        assertThatThrownBy(() -> service.findPage(-1, 20))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    void shouldThrowWhenSizeIsOutOfRange(int size) {
        assertThatThrownBy(() -> service.findPage(0, size))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    // регресія з Lab 1: page=2147483647 давав 500
    @Test
    void shouldReturnEmptyPageWhenOffsetIsTooLarge() {
        when(productRepository.count()).thenReturn(3L);

        PageResult<Product> result = service.findPage(Integer.MAX_VALUE, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    // ---------- upsert ----------

    @Test
    void shouldCreateProductWhenUpsertIdIsNew() {
        Product product = product("Galaxy yarn");
        when(categoryRepository.findByPublicId(categoryId)).thenReturn(Optional.of(categoryEntity));
        when(productRepository.findByCategoryPublicIdAndNameIgnoreCase(categoryId, "Galaxy yarn"))
                .thenReturn(Optional.empty());
        when(productRepository.findByPublicId(product.id())).thenReturn(Optional.empty());
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.upsert(product).created()).isTrue();
    }

    @Test
    void shouldUpdateProductWhenUpsertIdExists() {
        Product original = product("Galaxy yarn");
        ProductEntity entity = entityOf(original);
        Product changed = new Product(original.id(), "Galaxy yarn", "new", new BigDecimal("25.50"), categoryId);
        when(categoryRepository.findByPublicId(categoryId)).thenReturn(Optional.of(categoryEntity));
        // назву має цей самий продукт, тож це не конфлікт
        when(productRepository.findByCategoryPublicIdAndNameIgnoreCase(categoryId, "Galaxy yarn"))
                .thenReturn(Optional.of(entity));
        when(productRepository.findByPublicId(original.id())).thenReturn(Optional.of(entity));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(call -> call.getArgument(0));

        ProductService.UpsertResult result = service.upsert(changed);

        assertThat(result.created()).isFalse();
        assertThat(result.product().price()).isEqualByComparingTo("25.50");
    }

    // ---------- delete ----------

    @Test
    void shouldDeleteProduct() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(productRepository).deleteByPublicId(id);
    }
}