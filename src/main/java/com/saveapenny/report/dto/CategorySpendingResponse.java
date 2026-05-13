package com.saveapenny.report.dto;

import java.math.BigDecimal;
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
public class CategorySpendingResponse {

    private UUID categoryId;
    private String categoryName;
    private BigDecimal totalAmount;
    private BigDecimal usagePercentage;
}
