package com.saveapenny.category.exception;

import java.util.UUID;

public class SystemCategoryModificationNotAllowedException extends RuntimeException {

    public SystemCategoryModificationNotAllowedException(UUID categoryId) {
        super("System category cannot be modified or deleted: " + categoryId);
    }
}
