package com.saveapenny.stock.dto;

import java.util.List;
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
public class StockIncomeStatementResponse {

    private String symbol;
    private List<FinancialReportItem> annualReports;
    private List<FinancialReportItem> quarterlyReports;
}
