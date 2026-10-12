package com.cosmocats.cosmo_cats.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID id) {
        super("Product with id " + id + " not found");
    }
}
