package com.saveapenny.creditcard.dto;

import com.saveapenny.creditcard.entity.StatementStatus;
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
public class CreditCardSummaryResponse {

    private BigDecimal creditLimit;
    private BigDecimal apr;
    private Integer statementDay;
    private Integer gracePeriodDays;
    private BigDecimal availableCredit;
    private BigDecimal currentStatementBalance;
    private BigDecimal minimumPaymentDue;
    private LocalDate statementDate;
    private LocalDate paymentDueDate;
    private StatementStatus statementStatus;
}
