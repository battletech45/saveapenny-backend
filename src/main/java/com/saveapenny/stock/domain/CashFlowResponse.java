package com.saveapenny.stock.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CashFlowResponse(
        @JsonAlias("symbol") String symbol,
        @JsonAlias("annualReports") List<CashFlowItem> annualReports,
        @JsonAlias("quarterlyReports") List<CashFlowItem> quarterlyReports,
        @JsonAlias("Error Message") String errorMessage,
        @JsonAlias("Note") String note) {

    public CashFlowResponse(
            String symbol,
            List<CashFlowItem> annualReports,
            List<CashFlowItem> quarterlyReports) {
        this(symbol, annualReports, quarterlyReports, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CashFlowItem(
            @JsonAlias("fiscalDateEnding") String fiscalDateEnding,
            @JsonAlias("reportedCurrency") String reportedCurrency,
            @JsonAlias("operatingCashflow") String operatingCashflow,
            @JsonAlias("paymentsForOperatingActivities") String paymentsForOperatingActivities,
            @JsonAlias("proceedsFromOperatingActivities") String proceedsFromOperatingActivities,
            @JsonAlias("changeInOperatingLiabilities") String changeInOperatingLiabilities,
            @JsonAlias("changeInOperatingAssets") String changeInOperatingAssets,
            @JsonAlias("depreciationDepletionAndAmortization") String depreciationDepletionAndAmortization,
            @JsonAlias("capitalExpenditures") String capitalExpenditures,
            @JsonAlias("changeInReceivables") String changeInReceivables,
            @JsonAlias("changeInInventory") String changeInInventory,
            @JsonAlias("profitLoss") String profitLoss,
            @JsonAlias("cashflowFromInvestment") String cashflowFromInvestment,
            @JsonAlias("cashflowFromFinancing") String cashflowFromFinancing,
            @JsonAlias("proceedsFromRepaymentsOfShortTermDebt") String proceedsFromRepaymentsOfShortTermDebt,
            @JsonAlias("paymentsForRepurchaseOfCommonStock") String paymentsForRepurchaseOfCommonStock,
            @JsonAlias("paymentsForRepurchaseOfEquity") String paymentsForRepurchaseOfEquity,
            @JsonAlias("paymentsForRepurchaseOfPreferredStock") String paymentsForRepurchaseOfPreferredStock,
            @JsonAlias("dividendPayout") String dividendPayout,
            @JsonAlias("dividendPayoutCommonStock") String dividendPayoutCommonStock,
            @JsonAlias("dividendPayoutPreferredStock") String dividendPayoutPreferredStock,
            @JsonAlias("proceedsFromIssuanceOfCommonStock") String proceedsFromIssuanceOfCommonStock,
            @JsonAlias("proceedsFromIssuanceOfLongTermDebtAndCapitalSecuritiesNet") String proceedsFromIssuanceOfLongTermDebtAndCapitalSecuritiesNet,
            @JsonAlias("proceedsFromIssuanceOfPreferredStock") String proceedsFromIssuanceOfPreferredStock,
            @JsonAlias("proceedsFromRepurchaseOfEquity") String proceedsFromRepurchaseOfEquity,
            @JsonAlias("proceedsFromSaleOfTreasuryStock") String proceedsFromSaleOfTreasuryStock,
            @JsonAlias("changeInCashAndCashEquivalents") String changeInCashAndCashEquivalents,
            @JsonAlias("changeInExchangeRate") String changeInExchangeRate,
            @JsonAlias("netIncome") String netIncome) {}
}
