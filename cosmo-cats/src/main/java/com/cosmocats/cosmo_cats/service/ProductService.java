package com.cosmocats.cosmo_cats.service;

import com.cosmocats.cosmo_cats.domain.PageResult;
import com.cosmocats.cosmo_cats.domain.Product;
import com.cosmocats.cosmo_cats.exception.ProductAlreadyExistsException;
import com.cosmocats.cosmo_cats.exception.ProductNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProductService {

    // Mock storage until the database arrives in Lab 3
    private final Map<UUID, Product> products = new ConcurrentHashMap<>();

    public record UpsertResult(Product product, boolean created) {
    }

    public Product create(Product product) {
        boolean nameTaken = products.values().stream()
                .anyMatch(existing -> existing.name().equalsIgnoreCase(product.name()));
        if (nameTaken) {
            throw new ProductAlreadyExistsException(product.name());
        }
        products.put(product.id(), product);
        return product;
    }

    public Product findById(UUID id) {
        Product product = products.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    public PageResult<Product> findPage(int page, int size) {
        List<Product> all = products.values().stream()
                .sorted(Comparator.comparing(Product::name))
                .toList();

        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        int totalPages = (int) Math.ceil((double) all.size() / size);

        return new PageResult<>(all.subList(from, to), page, size, all.size(), totalPages);
    }

    public UpsertResult upsert(Product product) {
        boolean created = products.put(product.id(), product) == null;
        return new UpsertResult(product, created);
    }

    public void delete(UUID id) {
        products.remove(id); // idempotent: no error if it was already absent
    }
}