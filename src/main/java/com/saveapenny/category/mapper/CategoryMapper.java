package com.saveapenny.category.mapper;

import com.saveapenny.category.dto.CategoryResponse;
import com.saveapenny.category.dto.CreateCategoryRequest;
import com.saveapenny.category.dto.UpdateCategoryRequest;
import com.saveapenny.category.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Category category, UpdateCategoryRequest request);

    CategoryResponse toResponse(Category category);
}
