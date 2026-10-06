package com.cosmocats.cosmo_cats.mapper;

import com.cosmocats.cosmo_cats.domain.Product;
import com.cosmocats.cosmo_cats.dto.ProductRequest;
import com.cosmocats.cosmo_cats.dto.ProductResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductMapper {

    public ProductResponse toDto(Product product){
        return new ProductResponse(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.categoryId()
        );
    }

    public Product toDomain(ProductRequest request, UUID id){
        return new Product(
                id,
                request.name(),
                request.description(),
                request.price(),
                request.categoryId()
        );
    }

}
