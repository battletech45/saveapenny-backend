package com.saveapenny.transaction.dto;

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
public class TransactionResponse {

    private UUID id;
    private UUID userId;
    private UUID accountId;
    private UUID categoryId;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDate transactionDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
