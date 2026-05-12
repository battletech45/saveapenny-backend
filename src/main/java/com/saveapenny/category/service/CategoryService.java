package com.saveapenny.category.service;

import com.saveapenny.category.dto.CategoryResponse;
import com.saveapenny.category.dto.CreateCategoryRequest;
import com.saveapenny.category.dto.UpdateCategoryRequest;
import com.saveapenny.category.entity.CategoryType;
import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse create(UUID currentUserId, CreateCategoryRequest request);

    List<CategoryResponse> getAll(UUID currentUserId, CategoryType type);

    CategoryResponse getById(UUID currentUserId, UUID categoryId);

    CategoryResponse update(UUID currentUserId, UUID categoryId, UpdateCategoryRequest request);

    void delete(UUID currentUserId, UUID categoryId);
}
