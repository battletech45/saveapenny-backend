package com.saveapenny.automation.dto;

import com.saveapenny.automation.entity.RecurringExecutionStatus;
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
public class RecurringExecutionHistoryResponse {

    private UUID id;
    private UUID recurringTransactionId;
    private RecurringExecutionStatus status;
    private LocalDate scheduledDate;
    private OffsetDateTime executedAt;
    private UUID transactionId;
    private String failureReason;
}
