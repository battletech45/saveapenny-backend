package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BalanceSheetResponse(
        @JsonAlias("symbol") String symbol,
        @JsonAlias("annualReports") List<BalanceSheetItem> annualReports,
        @JsonAlias("quarterlyReports") List<BalanceSheetItem> quarterlyReports,
        @JsonAlias("Error Message") String errorMessage,
        @JsonAlias("Note") String note) {

    public BalanceSheetResponse(
            String symbol,
            List<BalanceSheetItem> annualReports,
            List<BalanceSheetItem> quarterlyReports) {
        this(symbol, annualReports, quarterlyReports, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BalanceSheetItem(
            @JsonAlias("fiscalDateEnding") String fiscalDateEnding,
            @JsonAlias("reportedCurrency") String reportedCurrency,
            @JsonAlias("totalAssets") String totalAssets,
            @JsonAlias("totalCurrentAssets") String totalCurrentAssets,
            @JsonAlias("cashAndCashEquivalentsAtCarryingValue") String cashAndCashEquivalentsAtCarryingValue,
            @JsonAlias("cashAndShortTermInvestments") String cashAndShortTermInvestments,
            @JsonAlias("inventory") String inventory,
            @JsonAlias("currentNetReceivables") String currentNetReceivables,
            @JsonAlias("totalNonCurrentAssets") String totalNonCurrentAssets,
            @JsonAlias("propertyPlantEquipment") String propertyPlantEquipment,
            @JsonAlias("accumulatedDepreciationAmortizationPPE") String accumulatedDepreciationAmortizationPPE,
            @JsonAlias("intangibleAssets") String intangibleAssets,
            @JsonAlias("intangibleAssetsExcludingGoodwill") String intangibleAssetsExcludingGoodwill,
            @JsonAlias("goodwill") String goodwill,
            @JsonAlias("investments") String investments,
            @JsonAlias("longTermInvestments") String longTermInvestments,
            @JsonAlias("shortTermInvestments") String shortTermInvestments,
            @JsonAlias("otherCurrentAssets") String otherCurrentAssets,
            @JsonAlias("otherNonCurrentAssets") String otherNonCurrentAssets,
            @JsonAlias("totalLiabilities") String totalLiabilities,
            @JsonAlias("totalCurrentLiabilities") String totalCurrentLiabilities,
            @JsonAlias("currentAccountsPayable") String currentAccountsPayable,
            @JsonAlias("deferredRevenue") String deferredRevenue,
            @JsonAlias("currentDebt") String currentDebt,
            @JsonAlias("shortTermDebt") String shortTermDebt,
            @JsonAlias("totalNonCurrentLiabilities") String totalNonCurrentLiabilities,
            @JsonAlias("capitalLeaseObligations") String capitalLeaseObligations,
            @JsonAlias("longTermDebt") String longTermDebt,
            @JsonAlias("currentLongTermDebt") String currentLongTermDebt,
            @JsonAlias("longTermDebtNoncurrent") String longTermDebtNoncurrent,
            @JsonAlias("shortLongTermDebtTotal") String shortLongTermDebtTotal,
            @JsonAlias("otherCurrentLiabilities") String otherCurrentLiabilities,
            @JsonAlias("otherNonCurrentLiabilities") String otherNonCurrentLiabilities,
            @JsonAlias("totalShareholderEquity") String totalShareholderEquity,
            @JsonAlias("treasuryStock") String treasuryStock,
            @JsonAlias("retainedEarnings") String retainedEarnings,
            @JsonAlias("commonStock") String commonStock,
            @JsonAlias("commonStockSharesOutstanding") String commonStockSharesOutstanding) {}
}
