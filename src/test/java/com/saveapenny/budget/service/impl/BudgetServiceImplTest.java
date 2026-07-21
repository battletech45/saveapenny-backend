package com.saveapenny.budget.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.analytics.service.AnalyticsEventPublisher;
import com.saveapenny.billing.service.BillingAccessService;
import com.saveapenny.budget.dto.BudgetResponse;
import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.dto.CreateBudgetRequest;
import com.saveapenny.budget.dto.UpdateBudgetRequest;
import com.saveapenny.budget.entity.Budget;
import com.saveapenny.budget.entity.BudgetPeriod;
import com.saveapenny.budget.exception.BudgetAlreadyExistsException;
import com.saveapenny.budget.exception.BudgetBatchDeleteException;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    @Mock
    private AnalyticsEventPublisher analyticsEventPublisher;
    @Mock
    private BillingAccessService billingAccessService;

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
    void delete_deletesOwnedBudget() {
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));

        budgetService.delete(userId, budgetId);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void delete_throws_whenNotOwned() {
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> budgetService.delete(userId, budgetId));
        verify(budgetRepository, never()).delete(any(Budget.class));
    }

    @Test
    void getStatus_returnsOnTrack_whenUsageBelowEighty() {
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).name("Food").userId(userId).build()));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
                userId, categoryId, TransactionType.EXPENSE, budget.getStartDate(), budget.getEndDate()))
                .thenReturn(new BigDecimal("100.0000"));

        BudgetStatusResponse result = budgetService.getStatus(userId, budgetId);

        assertEquals("ON_TRACK", result.getStatus());
        assertEquals(new BigDecimal("25.00"), result.getUsagePercentage());
    }

    @Test
    void getStatus_handlesNullSpentAmountAsZero() {
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).name("Food").userId(userId).build()));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
                userId, categoryId, TransactionType.EXPENSE, budget.getStartDate(), budget.getEndDate()))
                .thenReturn(null);

        BudgetStatusResponse result = budgetService.getStatus(userId, budgetId);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getSpentAmount()));
        assertEquals("ON_TRACK", result.getStatus());
    }

    @Test
    void getStatus_returnsExceeded_whenUsageAboveHundred() {
        when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).name("Food").userId(userId).build()));
        when(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
                userId, categoryId, TransactionType.EXPENSE, budget.getStartDate(), budget.getEndDate()))
                .thenReturn(new BigDecimal("450.0000"));

        BudgetStatusResponse result = budgetService.getStatus(userId, budgetId);

        assertEquals("EXCEEDED", result.getStatus());
        assertEquals(new BigDecimal("112.50"), result.getUsagePercentage());
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
    void getStatuses_batchesCategoryAndTransactionLookups() {
        UUID secondBudgetId = UUID.randomUUID();
        UUID secondCategoryId = UUID.randomUUID();
        Budget secondBudget = Budget.builder()
                .id(secondBudgetId)
                .userId(userId)
                .categoryId(secondCategoryId)
                .amount(new BigDecimal("200.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 5, 10))
                .endDate(LocalDate.of(2026, 5, 20))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();

        when(budgetRepository.findAllByUserIdAndPeriod(userId, BudgetPeriod.MONTHLY, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(budget, secondBudget)));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(
                Category.builder().id(categoryId).name("Food").userId(userId).build(),
                Category.builder().id(secondCategoryId).name("Transport").userId(userId).build()));
        when(transactionRepository.sumDailyAmountByUserIdAndCategoryIdsAndTypeAndTransactionDateBetween(
                any(UUID.class),
                any(List.class),
                any(TransactionType.class),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(List.of(
                        dailyTotal(categoryId, LocalDate.of(2026, 5, 10), new BigDecimal("50.00")),
                        dailyTotal(categoryId, LocalDate.of(2026, 5, 11), new BigDecimal("30.00")),
                        dailyTotal(secondCategoryId, LocalDate.of(2026, 5, 15), new BigDecimal("40.00"))));

        var result = budgetService.getStatuses(userId, BudgetPeriod.MONTHLY, PageRequest.of(0, 20));

        assertEquals(2, result.getContent().size());
        assertEquals("Food", result.getContent().get(0).getCategory());
        assertEquals(new BigDecimal("80.00"), result.getContent().get(0).getSpentAmount());
        assertEquals("Transport", result.getContent().get(1).getCategory());
        assertEquals(new BigDecimal("40.00"), result.getContent().get(1).getSpentAmount());
        verify(transactionRepository, never()).sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
                any(UUID.class), any(UUID.class), any(TransactionType.class), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getStatuses_usesUnknownCategoryWhenReferenceMissing() {
        when(budgetRepository.findAllByUserIdAndPeriod(userId, BudgetPeriod.MONTHLY, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(budget)));
        when(categoryRepository.findAllById(any())).thenReturn(List.of());
        when(transactionRepository.sumDailyAmountByUserIdAndCategoryIdsAndTypeAndTransactionDateBetween(
                any(UUID.class), any(List.class), any(TransactionType.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        var result = budgetService.getStatuses(userId, BudgetPeriod.MONTHLY, PageRequest.of(0, 20));

        assertEquals("Unknown category", result.getContent().get(0).getCategory());
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

    @Test
    void batchDelete_deletesAll_whenAllOwned() {
        Set<UUID> ids = Set.of(budgetId, UUID.randomUUID());
        when(budgetRepository.deleteAllByIdAndUserId(ids, userId)).thenReturn(2);

        budgetService.batchDelete(userId, ids);

        verify(budgetRepository).deleteAllByIdAndUserId(ids, userId);
    }

    @Test
    void batchDelete_throws_whenSomeNotOwned() {
        Set<UUID> ids = Set.of(budgetId, UUID.randomUUID());
        when(budgetRepository.deleteAllByIdAndUserId(ids, userId)).thenReturn(1);

        assertThrows(BudgetBatchDeleteException.class, () -> budgetService.batchDelete(userId, ids));
    }

    private TransactionRepository.CategoryDailyExpenseTotal dailyTotal(
            UUID categoryId,
            LocalDate transactionDate,
            BigDecimal totalAmount) {
        return new TransactionRepository.CategoryDailyExpenseTotal() {
            @Override
            public UUID getCategoryId() {
                return categoryId;
            }

            @Override
            public LocalDate getTransactionDate() {
                return transactionDate;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return totalAmount;
            }
        };
    }
}
