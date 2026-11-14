package com.cosmocats.cosmo_cats.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(UUID id) {
        super("Category with id " + id + " not found");
    }
}
