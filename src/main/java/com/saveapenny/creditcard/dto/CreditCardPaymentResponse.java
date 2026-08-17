package com.saveapenny.creditcard.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
public class CreditCardPaymentResponse {

    private UUID transactionId;
    private UUID creditAccountId;
    private UUID sourceAccountId;
    private BigDecimal amountPaid;
    private BigDecimal remainingBalance;
    private OffsetDateTime paidAt;
}
