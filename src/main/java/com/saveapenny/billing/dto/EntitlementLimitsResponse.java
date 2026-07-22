package com.saveapenny.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementLimitsResponse {

    private long activeBudgets;
    private Integer maxActiveBudgets;
    private long activeGoals;
    private Integer maxActiveGoals;
    private int reportHistoryMonths;
}
