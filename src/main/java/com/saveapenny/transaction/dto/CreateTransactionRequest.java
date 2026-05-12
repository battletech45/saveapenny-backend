package com.saveapenny.transaction.dto;

import com.saveapenny.transaction.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class CreateTransactionRequest {

    @NotNull
    private UUID accountId;

    @NotNull
    private UUID categoryId;

    @NotNull
    private TransactionType type;

    @NotNull
    @DecimalMin(value = "0.0001")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO currency code")
    private String currency;

    @Size(max = 500)
    private String description;

    @NotNull
    private LocalDate transactionDate;
}
