package com.saveapenny.category.dto;

import com.saveapenny.category.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class UpdateCategoryRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private CategoryType type;

    @Size(max = 20)
    private String color;

    @Size(max = 100)
    private String icon;

}
