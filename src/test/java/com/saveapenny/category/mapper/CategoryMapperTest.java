package com.saveapenny.category.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saveapenny.category.dto.CategoryResponse;
import com.saveapenny.category.dto.CreateCategoryRequest;
import com.saveapenny.category.dto.UpdateCategoryRequest;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.entity.CategoryType;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CategoryMapperTest {

    private final CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    @Test
    void toEntity_mapsCreateRequest() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Food")
                .type(CategoryType.EXPENSE)
                .color("#FF0000")
                .icon("utensils")
                .build();

        Category entity = categoryMapper.toEntity(request);

        assertNull(entity.getId());
        assertNull(entity.getUserId());
        assertEquals("Food", entity.getName());
        assertEquals(CategoryType.EXPENSE, entity.getType());
        assertEquals("#FF0000", entity.getColor());
        assertEquals("utensils", entity.getIcon());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void updateEntity_mapsFields() {
        Category category = Category.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .name("Old")
                .type(CategoryType.INCOME)
                .color("#000000")
                .icon("old-icon")
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now().minusDays(1))
                .build();

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Salary")
                .type(CategoryType.INCOME)
                .color("#00FF00")
                .icon("money-bill")
                .build();

        categoryMapper.updateEntity(category, request);

        assertEquals("Salary", category.getName());
        assertEquals(CategoryType.INCOME, category.getType());
        assertEquals("#00FF00", category.getColor());
        assertEquals("money-bill", category.getIcon());
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Category entity = Category.builder()
                .id(id)
                .userId(userId)
                .name("Transport")
                .type(CategoryType.EXPENSE)
                .color("#0000FF")
                .icon("car")
                .createdAt(now)
                .updatedAt(now)
                .build();

        CategoryResponse response = categoryMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals(userId, response.getUserId());
        assertEquals("Transport", response.getName());
        assertEquals(CategoryType.EXPENSE, response.getType());
        assertEquals("#0000FF", response.getColor());
        assertEquals("car", response.getIcon());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }
}
