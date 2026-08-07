package com.saveapenny.creditcard.dto;

import com.saveapenny.creditcard.entity.StatementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
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
public class CreditCardStatementResponse {

    private UUID id;
    private UUID accountId;
    private LocalDate statementDate;
    private LocalDate dueDate;
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private BigDecimal interestCharged;
    private BigDecimal minimumPaymentDue;
    private BigDecimal amountPaid;
    private StatementStatus status;
}
