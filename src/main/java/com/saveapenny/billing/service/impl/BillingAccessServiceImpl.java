package com.saveapenny.billing.service.impl;

import com.saveapenny.billing.entity.BillingEntitlement;
import com.saveapenny.billing.entity.EntitlementStatus;
import com.saveapenny.billing.entity.Plan;
import com.saveapenny.billing.exception.FreePlanLimitReachedException;
import com.saveapenny.billing.exception.PlusRequiredException;
import com.saveapenny.billing.exception.ReportHistoryLimitReachedException;
import com.saveapenny.billing.repository.BillingEntitlementRepository;
import com.saveapenny.billing.service.BillingAccessService;
import com.saveapenny.billing.service.PlanCapabilities;
import com.saveapenny.budget.repository.BudgetRepository;
import com.saveapenny.config.TimeService;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.repository.GoalRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingAccessServiceImpl implements BillingAccessService {

    private final BillingEntitlementRepository billingEntitlementRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final TimeService timeService;

    public BillingAccessServiceImpl(
            BillingEntitlementRepository billingEntitlementRepository,
            BudgetRepository budgetRepository,
            GoalRepository goalRepository,
            TimeService timeService) {
        this.billingEntitlementRepository = billingEntitlementRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.timeService = timeService;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireFeature(UUID userId, String featureName) {
        if (!hasPlusAccess(userId)) {
            throw new PlusRequiredException(featureName);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceBudgetCreationLimit(UUID userId) {
        if (hasPlusAccess(userId)) {
            return;
        }
        long activeBudgets = budgetRepository.countByUserIdAndEndDateGreaterThanEqual(userId, timeService.today());
        if (activeBudgets >= PlanCapabilities.FREE_MAX_ACTIVE_BUDGETS) {
            throw new FreePlanLimitReachedException("budget", PlanCapabilities.FREE_MAX_ACTIVE_BUDGETS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceGoalCreationLimit(UUID userId) {
        if (hasPlusAccess(userId)) {
            return;
        }
        long activeGoals = goalRepository.countByUserIdAndStatusAndDeletedAtIsNull(userId, GoalStatus.ACTIVE);
        if (activeGoals >= PlanCapabilities.FREE_MAX_ACTIVE_GOALS) {
            throw new FreePlanLimitReachedException("goal", PlanCapabilities.FREE_MAX_ACTIVE_GOALS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceReportHistoryWindow(UUID userId, LocalDate from) {
        if (hasPlusAccess(userId) || from == null) {
            return;
        }
        LocalDate floor = timeService.today().minusMonths(PlanCapabilities.FREE_REPORT_HISTORY_MONTHS);
        if (from.isBefore(floor)) {
            throw new ReportHistoryLimitReachedException(PlanCapabilities.FREE_REPORT_HISTORY_MONTHS);
        }
    }

    private boolean hasPlusAccess(UUID userId) {
        BillingEntitlement entitlement = billingEntitlementRepository.findById(userId).orElse(null);
        if (entitlement == null || entitlement.getPlan() != Plan.PLUS) {
            return false;
        }
        EntitlementStatus status = entitlement.effectiveStatus(timeService.now().atOffset(java.time.ZoneOffset.UTC));
        return status == EntitlementStatus.ACTIVE
                || status == EntitlementStatus.TRIALING
                || status == EntitlementStatus.GRACE_PERIOD;
    }
}
