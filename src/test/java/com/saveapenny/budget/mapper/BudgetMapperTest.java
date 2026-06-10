package com.saveapenny.budget.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saveapenny.budget.dto.BudgetResponse;
import com.saveapenny.budget.dto.CreateBudgetRequest;
import com.saveapenny.budget.dto.UpdateBudgetRequest;
import com.saveapenny.budget.entity.Budget;
import com.saveapenny.budget.entity.BudgetPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class BudgetMapperTest {

    private final BudgetMapper budgetMapper = Mappers.getMapper(BudgetMapper.class);

    @Test
    void toEntity_mapsCreateRequest() {
        UUID categoryId = UUID.randomUUID();
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(categoryId)
                .amount(new BigDecimal("400.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .build();

        Budget entity = budgetMapper.toEntity(request);

        assertNull(entity.getId());
        assertNull(entity.getUserId());
        assertEquals(categoryId, entity.getCategoryId());
        assertEquals(0, new BigDecimal("400.0000").compareTo(entity.getAmount()));
        assertEquals(BudgetPeriod.MONTHLY, entity.getPeriod());
        assertEquals(LocalDate.of(2026, 6, 1), entity.getStartDate());
        assertEquals(LocalDate.of(2026, 6, 30), entity.getEndDate());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void updateEntity_mapsFields() {
        Budget budget = Budget.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("100.0000"))
                .period(BudgetPeriod.YEARLY)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now().minusDays(1))
                .build();

        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("500.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 31))
                .build();

        budgetMapper.updateEntity(budget, request);

        assertEquals(request.getCategoryId(), budget.getCategoryId());
        assertEquals(0, request.getAmount().compareTo(budget.getAmount()));
        assertEquals(BudgetPeriod.MONTHLY, budget.getPeriod());
        assertEquals(request.getStartDate(), budget.getStartDate());
        assertEquals(request.getEndDate(), budget.getEndDate());
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Budget entity = Budget.builder()
                .id(id)
                .userId(userId)
                .categoryId(categoryId)
                .amount(new BigDecimal("800.0000"))
                .period(BudgetPeriod.YEARLY)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .createdAt(now)
                .updatedAt(now)
                .build();

        BudgetResponse response = budgetMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals(userId, response.getUserId());
        assertEquals(categoryId, response.getCategoryId());
        assertEquals(0, new BigDecimal("800.0000").compareTo(response.getAmount()));
        assertEquals(BudgetPeriod.YEARLY, response.getPeriod());
        assertEquals(LocalDate.of(2026, 1, 1), response.getStartDate());
        assertEquals(LocalDate.of(2026, 12, 31), response.getEndDate());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }
}
