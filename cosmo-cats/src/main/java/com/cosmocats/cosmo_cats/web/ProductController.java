package com.cosmocats.cosmo_cats.web;

import com.cosmocats.cosmo_cats.domain.Product;
import com.cosmocats.cosmo_cats.dto.ProductPageResponse;
import com.cosmocats.cosmo_cats.dto.ProductRequest;
import com.cosmocats.cosmo_cats.dto.ProductResponse;
import com.cosmocats.cosmo_cats.mapper.ProductMapper;
import com.cosmocats.cosmo_cats.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @GetMapping
    public ProductPageResponse getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productMapper.toPageDto(productService.findPage(page, size));
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable UUID id) {
        return productMapper.toDto(productService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {
        Product created = productService.create(productMapper.toDomain(request, UUID.randomUUID()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(productMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @RequestBody ProductRequest request) {
        ProductService.UpsertResult result = productService.upsert(productMapper.toDomain(request, id));
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(productMapper.toDto(result.product()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        productService.delete(id);
    }


}
