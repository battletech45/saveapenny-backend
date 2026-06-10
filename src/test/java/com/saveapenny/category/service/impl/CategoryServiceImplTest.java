package com.saveapenny.category.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private UUID userId;
    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .color("#FF0000")
                .icon("utensils")
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void create_returnsResponse_whenValid() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(" Food ")
                .type(CategoryType.EXPENSE)
                .color(" #FF0000 ")
                .icon(" utensils ")
                .build();
        Category mapped = Category.builder().type(CategoryType.EXPENSE).build();
        CategoryResponse response = CategoryResponse.builder().id(categoryId).name("Food").build();

        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, "Food", CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(mapped);
        when(categoryRepository.save(mapped)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(response);

        CategoryResponse result = categoryService.create(userId, request);

        assertEquals(categoryId, result.getId());
        assertEquals(userId, mapped.getUserId());
        assertEquals("Food", mapped.getName());
        assertEquals("#FF0000", mapped.getColor());
        assertEquals("utensils", mapped.getIcon());
    }

    @Test
    void create_throws_whenDuplicateNameExists() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Food")
                .type(CategoryType.EXPENSE)
                .build();

        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, "Food", CategoryType.EXPENSE))
                .thenReturn(true);

        assertThrows(CategoryNameAlreadyExistsException.class, () -> categoryService.create(userId, request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getById_throws_whenNotVisible() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.getById(userId, categoryId));
    }

    @Test
    void getById_returnsResponse_whenVisible() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().id(categoryId).name("Food").build());

        CategoryResponse result = categoryService.getById(userId, categoryId);

        assertEquals("Food", result.getName());
    }

    @Test
    void update_returnsResponse_whenValid() {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Groceries")
                .type(CategoryType.EXPENSE)
                .color("#00FF00")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(userId, "Groceries", CategoryType.EXPENSE, categoryId))
                .thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(CategoryResponse.builder().name("Groceries").build());

        CategoryResponse result = categoryService.update(userId, categoryId, request);

        assertEquals("Groceries", result.getName());
        assertEquals("#00FF00", category.getColor());
        verify(categoryMapper).updateEntity(category, request);
    }

    @Test
    void delete_returnsOk_whenOwned() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        categoryService.delete(userId, categoryId);

        verify(categoryRepository).delete(category);
    }

    @Test
    void update_throws_forSystemCategory() {
        Category systemCategory = Category.builder().id(categoryId).userId(null).name("System").build();
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("System2")
                .type(CategoryType.EXPENSE)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(systemCategory));

        assertThrows(SystemCategoryModificationNotAllowedException.class,
                () -> categoryService.update(userId, categoryId, request));
    }

    @Test
    void delete_throws_forSystemCategory() {
        Category systemCategory = Category.builder().id(categoryId).userId(null).name("System").build();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(systemCategory));

        assertThrows(SystemCategoryModificationNotAllowedException.class,
                () -> categoryService.delete(userId, categoryId));
    }

    @Test
    void getAll_returnsSortedResponses() {
        Category a = Category.builder().id(UUID.randomUUID()).name("Alpha").type(CategoryType.EXPENSE).build();
        Category z = Category.builder().id(UUID.randomUUID()).name("Zoo").type(CategoryType.EXPENSE).build();
        when(categoryRepository.findAllByUserIdIsNullOrUserIdAndType(userId, CategoryType.EXPENSE)).thenReturn(List.of(z, a));
        when(categoryMapper.toResponse(a)).thenReturn(CategoryResponse.builder().name("Alpha").build());
        when(categoryMapper.toResponse(z)).thenReturn(CategoryResponse.builder().name("Zoo").build());

        List<CategoryResponse> result = categoryService.getAll(userId, CategoryType.EXPENSE);

        assertEquals("Alpha", result.get(0).getName());
        assertEquals("Zoo", result.get(1).getName());
    }
}
