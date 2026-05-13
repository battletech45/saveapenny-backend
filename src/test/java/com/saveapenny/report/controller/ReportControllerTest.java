package com.saveapenny.report.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.SecurityConfig;
import com.saveapenny.report.dto.CashFlowPointResponse;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.dto.NetWorthSnapshotResponse;
import com.saveapenny.report.exception.InvalidReportDateRangeException;
import com.saveapenny.report.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getMonthlySummary_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-r1")).thenReturn(true);
        when(jwtService.extractUserId("token-r1")).thenReturn(userId);
        when(reportService.getMonthlySummary(userId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(MonthlySummaryResponse.builder()
                        .startDate(LocalDate.of(2026, 5, 1))
                        .endDate(LocalDate.of(2026, 5, 31))
                        .totalIncome(new BigDecimal("1200.0000"))
                        .totalExpense(new BigDecimal("700.0000"))
                        .netSavings(new BigDecimal("500.0000"))
                        .build());

        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .header("Authorization", "Bearer token-r1")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.netSavings").value(500.0));
    }

    @Test
    void getCategorySpending_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-r2")).thenReturn(true);
        when(jwtService.extractUserId("token-r2")).thenReturn(userId);
        when(reportService.getCategorySpending(userId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(CategorySpendingResponse.builder()
                        .categoryId(UUID.randomUUID())
                        .categoryName("Food")
                        .totalAmount(new BigDecimal("300.0000"))
                        .usagePercentage(new BigDecimal("25.00"))
                        .build()));

        mockMvc.perform(get("/api/v1/reports/category-spending")
                        .header("Authorization", "Bearer token-r2")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].categoryName").value("Food"));
    }

    @Test
    void getCashFlow_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-r3")).thenReturn(true);
        when(jwtService.extractUserId("token-r3")).thenReturn(userId);
        when(reportService.getCashFlow(userId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(CashFlowPointResponse.builder()
                        .date(LocalDate.of(2026, 5, 10))
                        .incomeAmount(new BigDecimal("200.0000"))
                        .expenseAmount(new BigDecimal("120.0000"))
                        .netAmount(new BigDecimal("80.0000"))
                        .build()));

        mockMvc.perform(get("/api/v1/reports/cash-flow")
                        .header("Authorization", "Bearer token-r3")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].netAmount").value(80.0));
    }

    @Test
    void getNetWorth_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-r4")).thenReturn(true);
        when(jwtService.extractUserId("token-r4")).thenReturn(userId);
        when(reportService.getNetWorth(userId, LocalDate.of(2026, 5, 31)))
                .thenReturn(NetWorthSnapshotResponse.builder()
                        .snapshotDate(LocalDate.of(2026, 5, 31))
                        .totalAssets(new BigDecimal("5000.0000"))
                        .totalLiabilities(new BigDecimal("1200.0000"))
                        .netWorth(new BigDecimal("3800.0000"))
                        .build());

        mockMvc.perform(get("/api/v1/reports/net-worth")
                        .header("Authorization", "Bearer token-r4")
                        .param("snapshotDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.netWorth").value(3800.0));
    }

    @Test
    void getMonthlySummary_returnsBadRequest_whenDateRangeInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-r5")).thenReturn(true);
        when(jwtService.extractUserId("token-r5")).thenReturn(userId);
        when(reportService.getMonthlySummary(userId, LocalDate.of(2026, 5, 31), LocalDate.of(2026, 5, 1)))
                .thenThrow(new InvalidReportDateRangeException(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 5, 1)));

        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .header("Authorization", "Bearer token-r5")
                        .param("from", "2026-05-31")
                        .param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REPORT_DATE_RANGE"));
    }
}
