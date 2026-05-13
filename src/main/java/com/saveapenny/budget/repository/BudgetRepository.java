package com.saveapenny.budget.repository;

import com.saveapenny.budget.entity.Budget;
import com.saveapenny.budget.entity.BudgetPeriod;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    Page<Budget> findAllByUserId(UUID userId, Pageable pageable);

    Page<Budget> findAllByUserIdAndPeriod(UUID userId, BudgetPeriod period, Pageable pageable);

    boolean existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDate(
            UUID userId,
            UUID categoryId,
            BudgetPeriod period,
            LocalDate startDate,
            LocalDate endDate);

    boolean existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDateAndIdNot(
            UUID userId,
            UUID categoryId,
            BudgetPeriod period,
            LocalDate startDate,
            LocalDate endDate,
            UUID id);
}
