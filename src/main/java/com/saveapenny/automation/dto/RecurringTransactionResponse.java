package com.saveapenny.automation.dto;

import com.saveapenny.automation.entity.RecurringClassification;
import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.entity.RecurringStatus;
import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class RecurringTransactionResponse {

    private UUID id;
    private UUID userId;
    private UUID accountId;
    private UUID categoryId;
    private TransactionType type;
    private BigDecimal amount;
    private RecurringFrequency frequency;
    private LocalDate nextRunDate;
    private RecurringStatus status;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private OffsetDateTime lastRunAt;
    private RecurringClassification classification;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
