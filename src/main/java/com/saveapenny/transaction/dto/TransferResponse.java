package com.saveapenny.transaction.dto;

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
public class TransferResponse {

    private UUID transactionId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private UUID categoryId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDate transactionDate;
    private OffsetDateTime createdAt;
}
