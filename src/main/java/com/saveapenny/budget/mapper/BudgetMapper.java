package com.saveapenny.budget.mapper;

import com.saveapenny.budget.dto.BudgetResponse;
import com.saveapenny.budget.dto.CreateBudgetRequest;
import com.saveapenny.budget.dto.UpdateBudgetRequest;
import com.saveapenny.budget.entity.Budget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BudgetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Budget toEntity(CreateBudgetRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Budget budget, UpdateBudgetRequest request);

    BudgetResponse toResponse(Budget budget);
}
