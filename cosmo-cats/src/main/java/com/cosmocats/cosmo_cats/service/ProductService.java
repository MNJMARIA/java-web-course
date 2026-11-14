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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// За замовчуванням методи тільки читають (readOnly); методи, що пишуть у базу, позначені окремо.
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public record UpsertResult(Product product, boolean created) {
    }

    @Transactional
    public Product create(Product product) {
        CategoryEntity category = findCategory(product.categoryId());
        checkNameIsFree(product);

        ProductEntity entity = new ProductEntity();
        entity.setPublicId(product.id());
        copyFields(entity, product, category);
        return toDomain(productRepository.save(entity));
    }

    public Product findById(UUID id) {
        return productRepository.findByPublicId(id)
                .map(this::toDomain)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public PageResult<Product> findPage(int page, int size) {
        if (page < 0) {
            throw new InvalidPageRequestException("Parameter 'page' must be 0 or greater.");
        }
        if (size < 1 || size > 100) {
            throw new InvalidPageRequestException("Parameter 'size' must be between 1 and 100.");
        }

        // Spring Data приймає зсув як int, тому для величезних сторінок
        // не питаємо базу, а повертаємо порожню сторінку (баг page=2147483647 з Lab 1)
        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE) {
            long total = productRepository.count();
            return new PageResult<>(List.of(), page, size, total, countPages(total, size));
        }

        Page<ProductEntity> result = productRepository.findAll(PageRequest.of(page, size, Sort.by("name")));
        List<Product> content = result.getContent().stream().map(this::toDomain).toList();
        return new PageResult<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public UpsertResult upsert(Product product) {
        CategoryEntity category = findCategory(product.categoryId());
        checkNameIsFree(product);

        Optional<ProductEntity> existing = productRepository.findByPublicId(product.id());
        ProductEntity entity = existing.orElseGet(ProductEntity::new);
        entity.setPublicId(product.id());
        copyFields(entity, product, category);

        ProductEntity saved = productRepository.save(entity);
        return new UpsertResult(toDomain(saved), existing.isEmpty());
    }

    // ідемпотентно: видалення неіснуючого продукту не є помилкою
    @Transactional
    public void delete(UUID id) {
        productRepository.deleteByPublicId(id);
    }

    private CategoryEntity findCategory(UUID categoryId) {
        return categoryRepository.findByPublicId(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    // 409, якщо ІНШИЙ продукт у цій категорії вже має таку назву
    // (останній захист від дубля це unique constraint у базі)
    private void checkNameIsFree(Product product) {
        productRepository.findByCategoryPublicIdAndNameIgnoreCase(product.categoryId(), product.name())
                .filter(existing -> !existing.getPublicId().equals(product.id()))
                .ifPresent(existing -> {
                    throw new ProductAlreadyExistsException(product.name());
                });
    }

    private void copyFields(ProductEntity entity, Product product, CategoryEntity category) {
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setPrice(product.price());
        entity.setCategory(category);
    }

    private Product toDomain(ProductEntity entity) {
        return new Product(entity.getPublicId(), entity.getName(), entity.getDescription(),
                entity.getPrice(), entity.getCategory().getPublicId());
    }

    private int countPages(long total, int size) {
        return (int) ((total + size - 1) / size);
    }
}