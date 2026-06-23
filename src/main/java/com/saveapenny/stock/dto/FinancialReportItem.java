package com.saveapenny.stock.dto;

import java.util.Map;
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
public class FinancialReportItem {

    private String fiscalDateEnding;
    private String reportedCurrency;
    private Map<String, String> fields;
}
