package com.saveapenny.automation.dto;

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
public class UpcomingRunResponse {

    private UUID recurringTransactionId;
    private String name;
    private BigDecimal amount;
    private LocalDate scheduledDate;
}
