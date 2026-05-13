package com.saveapenny.budget.service;

import com.saveapenny.budget.dto.BudgetResponse;
import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.dto.CreateBudgetRequest;
import com.saveapenny.budget.dto.UpdateBudgetRequest;
import com.saveapenny.budget.entity.BudgetPeriod;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BudgetService {

    BudgetResponse create(UUID currentUserId, CreateBudgetRequest request);

    Page<BudgetResponse> getAll(UUID currentUserId, BudgetPeriod period, Pageable pageable);

    BudgetResponse getById(UUID currentUserId, UUID budgetId);

    BudgetStatusResponse getStatus(UUID currentUserId, UUID budgetId);

    BudgetResponse update(UUID currentUserId, UUID budgetId, UpdateBudgetRequest request);

    void delete(UUID currentUserId, UUID budgetId);
}
