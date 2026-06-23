package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IncomeStatementResponse(
        @JsonAlias("symbol") String symbol,
        @JsonAlias("annualReports") List<IncomeReportItem> annualReports,
        @JsonAlias("quarterlyReports") List<IncomeReportItem> quarterlyReports,
        @JsonAlias("Error Message") String errorMessage,
        @JsonAlias("Note") String note) {

    public IncomeStatementResponse(
            String symbol,
            List<IncomeReportItem> annualReports,
            List<IncomeReportItem> quarterlyReports) {
        this(symbol, annualReports, quarterlyReports, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IncomeReportItem(
            @JsonAlias("fiscalDateEnding") String fiscalDateEnding,
            @JsonAlias("reportedCurrency") String reportedCurrency,
            @JsonAlias("grossProfit") String grossProfit,
            @JsonAlias("totalRevenue") String totalRevenue,
            @JsonAlias("costOfRevenue") String costOfRevenue,
            @JsonAlias("costofGoodsAndServicesSold") String costofGoodsAndServicesSold,
            @JsonAlias("operatingIncome") String operatingIncome,
            @JsonAlias("sellingGeneralAndAdministrative") String sellingGeneralAndAdministrative,
            @JsonAlias("researchAndDevelopment") String researchAndDevelopment,
            @JsonAlias("operatingExpenses") String operatingExpenses,
            @JsonAlias("investmentIncomeNet") String investmentIncomeNet,
            @JsonAlias("netInterestIncome") String netInterestIncome,
            @JsonAlias("interestIncome") String interestIncome,
            @JsonAlias("interestExpense") String interestExpense,
            @JsonAlias("nonInterestIncome") String nonInterestIncome,
            @JsonAlias("otherNonOperatingIncome") String otherNonOperatingIncome,
            @JsonAlias("depreciation") String depreciation,
            @JsonAlias("depreciationAndAmortization") String depreciationAndAmortization,
            @JsonAlias("incomeBeforeTax") String incomeBeforeTax,
            @JsonAlias("incomeTaxExpense") String incomeTaxExpense,
            @JsonAlias("interestAndDebtExpense") String interestAndDebtExpense,
            @JsonAlias("netIncomeFromContinuingOperations") String netIncomeFromContinuingOperations,
            @JsonAlias("comprehensiveIncomeNetOfTax") String comprehensiveIncomeNetOfTax,
            @JsonAlias("ebit") String ebit,
            @JsonAlias("ebitda") String ebitda,
            @JsonAlias("netIncome") String netIncome) {}
}
