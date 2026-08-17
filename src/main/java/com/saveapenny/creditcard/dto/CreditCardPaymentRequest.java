package com.saveapenny.creditcard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
public class CreditCardPaymentRequest {

    @NotNull
    private UUID sourceAccountId;

    @NotNull
    private PaymentType paymentType;

    @DecimalMin(value = "0.0001")
    private BigDecimal amount;
}
