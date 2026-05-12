package com.saveapenny.category.exception;

public class CategoryNameAlreadyExistsException extends RuntimeException {

    public CategoryNameAlreadyExistsException(String categoryName) {
        super("Category name is already in use: " + categoryName);
    }
}
