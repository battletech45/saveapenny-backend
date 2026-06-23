package com.saveapenny.stockholding.dto;

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
public class HoldingResponse {

    private UUID id;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private String currency;
    private LocalDate purchaseDate;
    private String notes;
    private BigDecimal investedAmount;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercent;
    private LocalDate latestTradingDay;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
