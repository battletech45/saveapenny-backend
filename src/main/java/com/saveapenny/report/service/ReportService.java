package com.saveapenny.report.service;

import com.saveapenny.report.dto.CashFlowPointResponse;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.dto.NetWorthSnapshotResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportService {

    MonthlySummaryResponse getMonthlySummary(UUID currentUserId, LocalDate from, LocalDate to);

    List<CategorySpendingResponse> getCategorySpending(UUID currentUserId, LocalDate from, LocalDate to);

    List<CashFlowPointResponse> getCashFlow(UUID currentUserId, LocalDate from, LocalDate to);

    NetWorthSnapshotResponse getNetWorth(UUID currentUserId, LocalDate snapshotDate);
}
