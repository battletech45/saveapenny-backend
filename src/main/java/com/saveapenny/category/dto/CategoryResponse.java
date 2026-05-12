package com.saveapenny.category.dto;

import com.saveapenny.category.entity.CategoryType;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private CategoryType type;
    private String color;
    private String icon;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
