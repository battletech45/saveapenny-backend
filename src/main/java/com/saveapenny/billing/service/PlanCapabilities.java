package com.saveapenny.billing.service;

import com.saveapenny.billing.dto.FeatureAccessResponse;
import com.saveapenny.billing.entity.Plan;

public final class PlanCapabilities {

    public static final int FREE_MAX_ACTIVE_BUDGETS = 3;
    public static final int FREE_MAX_ACTIVE_GOALS = 1;
    public static final int FREE_REPORT_HISTORY_MONTHS = 3;
    public static final int PLUS_REPORT_HISTORY_MONTHS = Integer.MAX_VALUE;

    private PlanCapabilities() {
    }

    public static FeatureAccessResponse featuresFor(Plan plan) {
        boolean plus = plan == Plan.PLUS;
        return FeatureAccessResponse.builder()
                .assistant(plus)
                .insights(plus)
                .stocks(plus)
                .ocr(plus)
                .csvImport(plus)
                .reportExport(plus)
                .advancedRecurring(plus)
                .goalWhatIf(plus)
                .build();
    }

    public static Integer maxActiveBudgets(Plan plan) {
        return plan == Plan.PLUS ? null : FREE_MAX_ACTIVE_BUDGETS;
    }

    public static Integer maxActiveGoals(Plan plan) {
        return plan == Plan.PLUS ? null : FREE_MAX_ACTIVE_GOALS;
    }

    public static int reportHistoryMonths(Plan plan) {
        return plan == Plan.PLUS ? PLUS_REPORT_HISTORY_MONTHS : FREE_REPORT_HISTORY_MONTHS;
    }
}
