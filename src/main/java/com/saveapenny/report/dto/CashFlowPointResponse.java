package com.saveapenny.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class CashFlowPointResponse {

    private LocalDate date;
    private BigDecimal incomeAmount;
    private BigDecimal expenseAmount;
    private BigDecimal netAmount;
}
