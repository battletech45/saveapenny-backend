package com.saveapenny.category.service.impl;

import com.saveapenny.category.dto.CategoryResponse;
import com.saveapenny.category.dto.CreateCategoryRequest;
import com.saveapenny.category.dto.UpdateCategoryRequest;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.entity.CategoryType;
import com.saveapenny.category.exception.CategoryNameAlreadyExistsException;
import com.saveapenny.category.exception.CategoryNotFoundException;
import com.saveapenny.category.exception.SystemCategoryModificationNotAllowedException;
import com.saveapenny.category.mapper.CategoryMapper;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.category.service.CategoryService;
import com.saveapenny.transaction.repository.TransactionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final TransactionRepository transactionRepository;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            CategoryMapper categoryMapper,
            TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public CategoryResponse create(UUID currentUserId, CreateCategoryRequest request) {
        String normalizedName = normalizeName(request.getName());
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(currentUserId, normalizedName, request.getType())) {
            throw new CategoryNameAlreadyExistsException(normalizedName);
        }

        Category category = categoryMapper.toEntity(request);
        category.setUserId(currentUserId);
        category.setName(normalizedName);
        category.setColor(normalizeOptional(request.getColor()));
        category.setIcon(normalizeOptional(request.getIcon()));

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(UUID currentUserId, CategoryType type) {
        List<Category> categories = new ArrayList<>(categoryRepository.findAllByUserIdIsNullOrUserIdAndType(currentUserId, type));
        categories.sort(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER));
        return categories.stream().map(categoryMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID currentUserId, UUID categoryId) {
        Category category = findVisibleCategory(currentUserId, categoryId);
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse update(UUID currentUserId, UUID categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (category.getUserId() == null) {
            throw new SystemCategoryModificationNotAllowedException(categoryId);
        }

        if (!category.getUserId().equals(currentUserId)) {
            throw new CategoryNotFoundException(categoryId);
        }
        if (category.getType() != request.getType()
                && transactionRepository.existsByUserIdAndCategoryId(currentUserId, categoryId)) {
            throw new SystemCategoryModificationNotAllowedException(categoryId);
        }

        String normalizedName = normalizeName(request.getName());
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(
                currentUserId,
                normalizedName,
                request.getType(),
                categoryId)) {
            throw new CategoryNameAlreadyExistsException(normalizedName);
        }

        categoryMapper.updateEntity(category, request);
        category.setName(normalizedName);
        category.setColor(normalizeOptional(request.getColor()));
        category.setIcon(normalizeOptional(request.getIcon()));

        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID currentUserId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (category.getUserId() == null) {
            throw new SystemCategoryModificationNotAllowedException(categoryId);
        }

        if (!category.getUserId().equals(currentUserId)) {
            throw new CategoryNotFoundException(categoryId);
        }

        categoryRepository.delete(category);
    }

    private Category findVisibleCategory(UUID currentUserId, UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(category -> category.getUserId() == null || category.getUserId().equals(currentUserId))
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
