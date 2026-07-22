package com.saveapenny.billing.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Enforces plan-based access: feature gating and free-tier usage caps.
 * Distinct from feature-flag checks (globally disabled -> 503); this is per-user plan enforcement (-> 403).
 */
public interface BillingAccessService {

    void requireFeature(UUID userId, String featureName);

    void enforceBudgetCreationLimit(UUID userId);

    void enforceGoalCreationLimit(UUID userId);

    void enforceReportHistoryWindow(UUID userId, LocalDate from);
}
