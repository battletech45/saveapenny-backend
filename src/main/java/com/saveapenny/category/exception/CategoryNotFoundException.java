package com.saveapenny.category.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID categoryId) {
        super("Category not found: " + categoryId);
    }
}
