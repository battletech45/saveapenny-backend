package com.saveapenny.report.controller;

import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.report.dto.CashFlowPointResponse;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.dto.NetWorthSnapshotResponse;
import com.saveapenny.report.service.ReportService;
import com.saveapenny.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Reports", description = "Financial reporting and CSV export endpoints.")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<ApiResponse<MonthlySummaryResponse>> getMonthlySummary(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        MonthlySummaryResponse response = reportService.getMonthlySummary(getCurrentUserId(principal), from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(value = "/monthly-summary/export", produces = "text/csv")
    public ResponseEntity<ByteArrayResource> exportMonthlySummaryCsv(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        byte[] csv = reportService.exportMonthlySummaryCsv(getCurrentUserId(principal), from, to);
        ByteArrayResource resource = new ByteArrayResource(csv);
        String filename = "monthly-summary-%s-to-%s.csv".formatted(from, to);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @GetMapping("/category-spending")
    public ResponseEntity<ApiResponse<List<CategorySpendingResponse>>> getCategorySpending(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        List<CategorySpendingResponse> response = reportService.getCategorySpending(getCurrentUserId(principal), from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/cash-flow")
    public ResponseEntity<ApiResponse<List<CashFlowPointResponse>>> getCashFlow(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        List<CashFlowPointResponse> response = reportService.getCashFlow(getCurrentUserId(principal), from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/net-worth")
    public ResponseEntity<ApiResponse<NetWorthSnapshotResponse>> getNetWorth(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam LocalDate snapshotDate) {
        NetWorthSnapshotResponse response = reportService.getNetWorth(getCurrentUserId(principal), snapshotDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
