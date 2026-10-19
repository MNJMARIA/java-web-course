package com.cosmocats.cosmo_cats.service;

import com.cosmocats.cosmo_cats.domain.PageResult;
import com.cosmocats.cosmo_cats.domain.Product;
import com.cosmocats.cosmo_cats.exception.InvalidPageRequestException;
import com.cosmocats.cosmo_cats.exception.ProductAlreadyExistsException;
import com.cosmocats.cosmo_cats.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class ProductServiceTest {

    private ProductService service;
    private final UUID categoryId = UUID.randomUUID();

    // новий порожній сервіс перед кожним тестом, щоб тести не залежали один від одного
    @BeforeEach
    void setUp() {
        service = new ProductService();
    }

    private Product product(String name) {
        return new Product(UUID.randomUUID(), name, "test", new BigDecimal("19.99"), categoryId);
    }

    // ---------- create ----------

    @Test
    @DisplayName("create saves a product")
    void shouldCreateProduct() {
        Product product = product("Galaxy yarn");

        Product created = service.create(product);

        assertThat(created).isEqualTo(product);
        assertThat(service.findById(product.id())).isEqualTo(product);
    }

    @Test
    @DisplayName("create rejects a duplicate name, ignoring letter case")
    void shouldThrowConflictWhenNameAlreadyExists() {
        service.create(product("Galaxy yarn"));

        assertThatThrownBy(() -> service.create(product("GALAXY YARN")))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    // ---------- findById ----------

    @Test
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        UUID unknownId = UUID.randomUUID();

        assertThatThrownBy(() -> service.findById(unknownId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    // ---------- findPage ----------

    @Test
    @DisplayName("findPage splits products into pages sorted by name")
    void shouldReturnPages() {
        service.create(product("Nova"));
        service.create(product("Aster"));
        service.create(product("Comet"));

        PageResult<Product> first = service.findPage(0, 2);
        PageResult<Product> second = service.findPage(1, 2);

        assertThat(first.content()).extracting(Product::name).containsExactly("Aster", "Comet");
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(second.content()).extracting(Product::name).containsExactly("Nova");
    }

    @Test
    void shouldReturnEmptyContentWhenPageIsBeyondTheEnd() {
        service.create(product("Aster"));

        PageResult<Product> result = service.findPage(5, 2);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyPageWhenNoProducts() {
        PageResult<Product> result = service.findPage(0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void shouldThrowWhenPageIsNegative() {
        assertThatThrownBy(() -> service.findPage(-1, 20))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    // один тест, три значення: size виходить за межі 1..100
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    void shouldThrowWhenSizeIsOutOfRange(int size) {
        assertThatThrownBy(() -> service.findPage(0, size))
                .isInstanceOf(InvalidPageRequestException.class);
    }

    @Test
    void shouldNotOverflowWhenPageIsHuge() {
        PageResult<Product> result = service.findPage(Integer.MAX_VALUE, 20);

        assertThat(result.content()).isEmpty();
    }

    // ---------- upsert ----------

    @Test
    void shouldCreateProductWhenUpsertIdIsNew() {
        ProductService.UpsertResult result = service.upsert(product("Galaxy yarn"));

        assertThat(result.created()).isTrue();
    }

    @Test
    void shouldUpdateProductWhenUpsertIdExists() {
        Product original = product("Galaxy yarn");
        service.create(original);
        // той самий id і та сама назва, змінилась ціна
        Product changed = new Product(original.id(), "Galaxy yarn", "new", new BigDecimal("25.50"), categoryId);

        ProductService.UpsertResult result = service.upsert(changed);

        assertThat(result.created()).isFalse();
        assertThat(service.findById(original.id()).price()).isEqualByComparingTo("25.50");
    }

    @Test
    void shouldThrowConflictWhenUpsertTakesNameOfAnotherProduct() {
        service.create(product("Galaxy yarn"));
        Product other = product("Comet milk");
        service.create(other);
        Product renamed = new Product(other.id(), "Galaxy yarn", "x", new BigDecimal("1.00"), categoryId);

        assertThatThrownBy(() -> service.upsert(renamed))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    // ---------- delete ----------

    @Test
    void shouldDeleteProduct() {
        Product product = product("Galaxy yarn");
        service.create(product);

        service.delete(product.id());

        assertThatThrownBy(() -> service.findById(product.id()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("delete is idempotent: deleting a missing product does not fail")
    void shouldNotFailWhenDeletingMissingProduct() {
        service.delete(UUID.randomUUID());
    }
}