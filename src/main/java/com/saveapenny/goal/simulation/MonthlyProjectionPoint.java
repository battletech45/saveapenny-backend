package com.saveapenny.goal.simulation;

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
public class MonthlyProjectionPoint {

    private LocalDate month;
    private BigDecimal balance;
    private BigDecimal contribution;
    private BigDecimal interest;
    private BigDecimal payment;
    private BigDecimal interestCharged;
    private BigDecimal growth;
}
