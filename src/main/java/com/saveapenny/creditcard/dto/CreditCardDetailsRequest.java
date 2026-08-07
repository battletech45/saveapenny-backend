package com.saveapenny.creditcard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
public class CreditCardDetailsRequest {

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal creditLimit;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal apr;

    @NotNull
    @Min(1)
    @Max(28)
    private Integer statementDay;
}
