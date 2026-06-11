package com.saveapenny.budget.service.impl;

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
import com.saveapenny.budget.service.BudgetService;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.exception.CategoryNotFoundException;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BudgetServiceImpl implements BudgetService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("80");

    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public BudgetServiceImpl(
            BudgetRepository budgetRepository,
            BudgetMapper budgetMapper,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.budgetMapper = budgetMapper;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public BudgetResponse create(UUID currentUserId, CreateBudgetRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        ensureCategoryVisible(currentUserId, request.getCategoryId());

        boolean exists = budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDate(
                currentUserId,
                request.getCategoryId(),
                request.getPeriod(),
                request.getStartDate(),
                request.getEndDate());
        if (exists) {
            throw new BudgetAlreadyExistsException();
        }

        Budget budget = budgetMapper.toEntity(request);
        budget.setUserId(currentUserId);

        Budget saved = budgetRepository.save(budget);
        return budgetMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BudgetResponse> getAll(UUID currentUserId, BudgetPeriod period, Pageable pageable) {
        Page<Budget> page = period == null
                ? budgetRepository.findAllByUserId(currentUserId, pageable)
                : budgetRepository.findAllByUserIdAndPeriod(currentUserId, period, pageable);
        return page.map(budgetMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getById(UUID currentUserId, UUID budgetId) {
        Budget budget = findOwnedBudget(currentUserId, budgetId);
        return budgetMapper.toResponse(budget);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetStatusResponse getStatus(UUID currentUserId, UUID budgetId) {
        Budget budget = findOwnedBudget(currentUserId, budgetId);
        Category category = ensureCategoryVisible(currentUserId, budget.getCategoryId());
        BigDecimal spentAmount = loadSpentAmount(currentUserId, budget);

        return toStatusResponse(budget, category.getName(), spentAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BudgetStatusResponse> getStatuses(UUID currentUserId, BudgetPeriod period, Pageable pageable) {
        Page<Budget> budgets = period == null
                ? budgetRepository.findAllByUserId(currentUserId, pageable)
                : budgetRepository.findAllByUserIdAndPeriod(currentUserId, period, pageable);

        if (budgets.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, budgets.getTotalElements());
        }

        Map<UUID, String> categoryNames = loadVisibleCategoryNames(currentUserId, budgets.getContent());
        Map<UUID, Map<LocalDate, BigDecimal>> dailyExpenseTotals = loadDailyExpenseTotals(currentUserId, budgets.getContent());

        return budgets.map(budget -> toStatusResponse(
                budget,
                resolveCategoryName(categoryNames, budget.getCategoryId()),
                calculateSpentAmount(budget, dailyExpenseTotals.getOrDefault(budget.getCategoryId(), Map.of()))));
    }

    @Override
    public BudgetResponse update(UUID currentUserId, UUID budgetId, UpdateBudgetRequest request) {
        Budget budget = findOwnedBudget(currentUserId, budgetId);
        validateDateRange(request.getStartDate(), request.getEndDate());
        ensureCategoryVisible(currentUserId, request.getCategoryId());

        boolean exists = budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDateAndIdNot(
                currentUserId,
                request.getCategoryId(),
                request.getPeriod(),
                request.getStartDate(),
                request.getEndDate(),
                budgetId);
        if (exists) {
            throw new BudgetAlreadyExistsException();
        }

        budgetMapper.updateEntity(budget, request);
        Budget saved = budgetRepository.save(budget);
        return budgetMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID currentUserId, UUID budgetId) {
        Budget budget = findOwnedBudget(currentUserId, budgetId);
        budgetRepository.delete(budget);
    }

    @Override
    public void batchDelete(UUID currentUserId, Set<UUID> budgetIds) {
        int deleted = budgetRepository.deleteAllByIdAndUserId(budgetIds, currentUserId);
        if (deleted != budgetIds.size()) {
            throw new BudgetBatchDeleteException(budgetIds.size(), deleted);
        }
    }

    private Budget findOwnedBudget(UUID currentUserId, UUID budgetId) {
        return budgetRepository.findByIdAndUserId(budgetId, currentUserId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));
    }

    private Category ensureCategoryVisible(UUID currentUserId, UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(category -> category.getUserId() == null || category.getUserId().equals(currentUserId))
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private Map<UUID, String> loadVisibleCategoryNames(UUID currentUserId, List<Budget> budgets) {
        Set<UUID> categoryIds = budgets.stream()
                .map(Budget::getCategoryId)
                .collect(java.util.stream.Collectors.toSet());

        return categoryRepository.findAllById(categoryIds).stream()
                .filter(category -> category.getUserId() == null || category.getUserId().equals(currentUserId))
                .collect(java.util.stream.Collectors.toMap(Category::getId, Category::getName));
    }

    private String resolveCategoryName(Map<UUID, String> categoryNames, UUID categoryId) {
        String categoryName = categoryNames.get(categoryId);
        if (categoryName == null) {
            return "Unknown category";
        }
        return categoryName;
    }

    private Map<UUID, Map<LocalDate, BigDecimal>> loadDailyExpenseTotals(UUID currentUserId, List<Budget> budgets) {
        Set<UUID> categoryIds = budgets.stream()
                .map(Budget::getCategoryId)
                .collect(java.util.stream.Collectors.toSet());
        LocalDate minStartDate = budgets.stream()
                .map(Budget::getStartDate)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate maxEndDate = budgets.stream()
                .map(Budget::getEndDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        if (categoryIds.isEmpty() || minStartDate == null || maxEndDate == null) {
            return Map.of();
        }

        Map<UUID, Map<LocalDate, BigDecimal>> totals = new HashMap<>();
        for (TransactionRepository.CategoryDailyExpenseTotal item : transactionRepository
                .sumDailyAmountByUserIdAndCategoryIdsAndTypeAndTransactionDateBetween(
                        currentUserId,
                        List.copyOf(categoryIds),
                        TransactionType.EXPENSE,
                        minStartDate,
                        maxEndDate)) {
            totals.computeIfAbsent(item.getCategoryId(), ignored -> new HashMap<>())
                    .put(item.getTransactionDate(), nullSafeAmount(item.getTotalAmount()));
        }
        return totals;
    }

    private BigDecimal calculateSpentAmount(Budget budget, Map<LocalDate, BigDecimal> dailyTotals) {
        BigDecimal spentAmount = BigDecimal.ZERO;
        for (LocalDate date = budget.getStartDate(); !date.isAfter(budget.getEndDate()); date = date.plusDays(1)) {
            spentAmount = spentAmount.add(nullSafeAmount(dailyTotals.get(date)));
        }
        return spentAmount;
    }

    private BigDecimal loadSpentAmount(UUID currentUserId, Budget budget) {
        return nullSafeAmount(transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
                currentUserId,
                budget.getCategoryId(),
                TransactionType.EXPENSE,
                budget.getStartDate(),
                budget.getEndDate()));
    }

    private BudgetStatusResponse toStatusResponse(Budget budget, String categoryName, BigDecimal spentAmount) {
        BigDecimal budgetAmount = nullSafeAmount(budget.getAmount());
        BigDecimal remainingAmount = budgetAmount.subtract(spentAmount);
        BigDecimal usagePercentage = calculateUsagePercentage(spentAmount, budgetAmount);

        return BudgetStatusResponse.builder()
                .category(categoryName)
                .budgetAmount(budgetAmount)
                .spentAmount(spentAmount)
                .remainingAmount(remainingAmount)
                .usagePercentage(usagePercentage)
                .status(resolveStatus(usagePercentage))
                .build();
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidBudgetDateRangeException(startDate, endDate);
        }
    }

    private BigDecimal calculateUsagePercentage(BigDecimal spentAmount, BigDecimal budgetAmount) {
        if (budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount
                .multiply(HUNDRED)
                .divide(budgetAmount, 2, RoundingMode.HALF_UP);
    }

    private String resolveStatus(BigDecimal usagePercentage) {
        if (usagePercentage.compareTo(HUNDRED) > 0) {
            return "EXCEEDED";
        }
        if (usagePercentage.compareTo(WARNING_THRESHOLD) >= 0) {
            return "WARNING";
        }
        return "ON_TRACK";
    }

    private BigDecimal nullSafeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
