package com.saveapenny.budget.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.budget.dto.BudgetResponse;
import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.dto.CreateBudgetRequest;
import com.saveapenny.budget.dto.UpdateBudgetRequest;
import com.saveapenny.budget.entity.Budget;
import com.saveapenny.budget.entity.BudgetPeriod;
import com.saveapenny.budget.exception.BudgetAlreadyExistsException;
import com.saveapenny.budget.exception.BudgetNotFoundException;
import com.saveapenny.budget.exception.InvalidBudgetDateRangeException;
import com.saveapenny.budget.mapper.BudgetMapper;
import com.saveapenny.budget.repository.BudgetRepository;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private BudgetMapper budgetMapper;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private UUID userId;
    private UUID budgetId;
    private UUID categoryId;
    private Budget budget;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        budgetId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        budget = Budget.builder()
                .id(budgetId)
                .userId(userId)
                .categoryId(categoryId)
                .amount(new BigDecimal("400.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void create_returnsResponse_whenValid() {
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(categoryId)
                .amount(new BigDecimal("400.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .build();
        Budget mapped = Budget.builder().amount(request.getAmount()).period(request.getPeriod()).build();
        BudgetResponse response = BudgetResponse.builder().id(budgetId).build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).userId(userId).build()));
        when(budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDate(
                userId, categoryId, BudgetPeriod.MONTHLY, request.getStartDate(), request.getEndDate())).thenReturn(false);
        when(budgetMapper.toEntity(request)).thenReturn(mapped);
        when(budgetRepository.save(mapped)).thenReturn(budget);
        when(budgetMapper.toResponse(budget)).thenReturn(response);

        BudgetResponse result = budgetService.create(userId, request);

        assertEquals(budgetId, result.getId());
        assertEquals(userId, mapped.getUserId());
    }

    @Test
    void create_throws_whenDuplicateExists() {
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(categoryId)
                .amount(new BigDecimal("400.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).userId(userId).build()));
        when(budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDate(
                userId, categoryId, BudgetPeriod.MONTHLY, request.getStartDate(), request.getEndDate())).thenReturn(true);

        assertThrows(BudgetAlreadyExistsException.class, () -> budgetService.create(userId, request));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void create_throws_whenDateRangeInvalid() {
        CreateBudgetRequest request = CreateBudgetRequest.builder()
                .categoryId(categoryId)
                .amount(new BigDecimal("100.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 31))
                .endDate(LocalDate.of(2026, 5, 1))
                .build();

        assertThrows(InvalidBudgetDateRangeException.class, () -> budgetService.create(userId, request));
    }

    @Test
    void getById_throws_whenNotOwned() {
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> budgetService.getById(userId, budgetId));
    }

    @Test
    void getAll_filtersByPeriod_whenProvided() {
        when(budgetRepository.findAllByUserIdAndPeriod(userId, BudgetPeriod.MONTHLY, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of(budget)));
        when(budgetMapper.toResponse(budget)).thenReturn(BudgetResponse.builder().id(budgetId).build());

        var result = budgetService.getAll(userId, BudgetPeriod.MONTHLY, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(budgetRepository).findAllByUserIdAndPeriod(userId, BudgetPeriod.MONTHLY, PageRequest.of(0, 20));
    }

    @Test
    void getStatus_returnsWarning_whenUsageAtOrAboveEighty() {
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).name("Food").userId(userId).build()));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
                userId,
                categoryId,
                TransactionType.EXPENSE,
                budget.getStartDate(),
                budget.getEndDate())).thenReturn(new BigDecimal("320.0000"));

        BudgetStatusResponse result = budgetService.getStatus(userId, budgetId);

        assertEquals("Food", result.getCategory());
        assertEquals(new BigDecimal("80.00"), result.getUsagePercentage());
        assertEquals("WARNING", result.getStatus());
    }

    @Test
    void update_throws_whenDuplicateExists() {
        UpdateBudgetRequest request = UpdateBudgetRequest.builder()
                .categoryId(categoryId)
                .amount(new BigDecimal("300.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .build();

        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).userId(userId).build()));
        when(budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDateAndIdNot(
                userId,
                categoryId,
                BudgetPeriod.MONTHLY,
                request.getStartDate(),
                request.getEndDate(),
                budgetId)).thenReturn(true);

        assertThrows(BudgetAlreadyExistsException.class, () -> budgetService.update(userId, budgetId, request));
        verify(budgetMapper, never()).updateEntity(any(Budget.class), any(UpdateBudgetRequest.class));
    }
}
