package com.saveapenny.automation.dto;

import com.saveapenny.automation.entity.RecurringClassification;
import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
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
public class CreateRecurringTransactionRequest {

    @NotNull
    private UUID accountId;

    @NotNull
    private UUID categoryId;

    @NotNull
    private TransactionType type;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private RecurringFrequency frequency;

    @NotNull
    private LocalDate nextRunDate;

    @Size(max = 150)
    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private RecurringClassification classification;
}
