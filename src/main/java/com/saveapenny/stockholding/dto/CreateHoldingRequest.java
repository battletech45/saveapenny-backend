package com.saveapenny.stockholding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class CreateHoldingRequest {

    @NotBlank
    @Size(max = 10)
    private String symbol;

    @NotNull
    @DecimalMin("0.00000001")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.0001")
    private BigDecimal purchasePrice;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO currency code")
    private String currency;

    @NotNull
    @PastOrPresent
    private LocalDate purchaseDate;

    @Size(max = 500)
    private String notes;
}
