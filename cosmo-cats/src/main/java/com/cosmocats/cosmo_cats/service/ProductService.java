package com.cosmocats.cosmo_cats.service;

import com.cosmocats.cosmo_cats.domain.PageResult;
import com.cosmocats.cosmo_cats.domain.Product;
import com.cosmocats.cosmo_cats.exception.InvalidPageRequestException;
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

    // synchronized: поки один запит виконує метод, інші чекають
    public synchronized Product create(Product product) {
        checkNameIsFree(product.name(), null);
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
        // Межі беремо з контракту: page >= 0, size від 1 до 100
        if (page < 0) {
            throw new InvalidPageRequestException("Parameter 'page' must be 0 or greater.");
        }
        if (size < 1 || size > 100) {
            throw new InvalidPageRequestException("Parameter 'size' must be between 1 and 100.");
        }

        // Сортуємо за назвою, щоб порядок не мінявся між запитами
        List<Product> all = products.values().stream()
                .sorted(Comparator.comparing(Product::name))
                .toList();

        // long, бо page * size у звичайному int може переповнитися
        long offset = (long) page * size;
        int from = (int) Math.min(offset, all.size());
        int to = Math.min(from + size, all.size());

        // Ділення з округленням вгору: size тут точно >= 1, ділення на 0 неможливе
        int totalPages = (all.size() + size - 1) / size;

        return new PageResult<>(all.subList(from, to), page, size, all.size(), totalPages);
    }

    public synchronized UpsertResult upsert(Product product) {
        // product.id() передаємо, щоб оновлення продукту з його ж назвою не вважалося дублем
        checkNameIsFree(product.name(), product.id());
        boolean created = products.put(product.id(), product) == null;
        return new UpsertResult(product, created);
    }

    public void delete(UUID id) {
        products.remove(id); // idempotent: no error if it was already absent
    }


    // Кидає 409, якщо назву вже має ІНШИЙ продукт
    private void checkNameIsFree(String name, UUID ownId) {
        for (Product existing : products.values()) {
            boolean sameName = existing.name().equalsIgnoreCase(name);
            boolean otherProduct = !existing.id().equals(ownId);
            if (sameName && otherProduct) {
                throw new ProductAlreadyExistsException(name);
            }
        }
    }
}