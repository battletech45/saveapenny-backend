package com.saveapenny.budget.service.impl;

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
import com.saveapenny.budget.service.BudgetService;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.exception.CategoryNotFoundException;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.data.domain.Page;
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

        BigDecimal spentAmount = transactionRepository.sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
                currentUserId,
                budget.getCategoryId(),
                TransactionType.EXPENSE,
                budget.getStartDate(),
                budget.getEndDate());

        BigDecimal budgetAmount = nullSafeAmount(budget.getAmount());
        BigDecimal remainingAmount = budgetAmount.subtract(spentAmount);
        BigDecimal usagePercentage = calculateUsagePercentage(spentAmount, budgetAmount);

        return BudgetStatusResponse.builder()
                .category(category.getName())
                .budgetAmount(budgetAmount)
                .spentAmount(spentAmount)
                .remainingAmount(remainingAmount)
                .usagePercentage(usagePercentage)
                .status(resolveStatus(usagePercentage))
                .build();
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

    private Budget findOwnedBudget(UUID currentUserId, UUID budgetId) {
        return budgetRepository.findByIdAndUserId(budgetId, currentUserId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));
    }

    private Category ensureCategoryVisible(UUID currentUserId, UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(category -> category.getUserId() == null || category.getUserId().equals(currentUserId))
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
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
