package com.saveapenny.billing.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.saveapenny.billing.entity.BillingEntitlement;
import com.saveapenny.billing.entity.EntitlementStatus;
import com.saveapenny.billing.entity.Plan;
import com.saveapenny.billing.exception.FreePlanLimitReachedException;
import com.saveapenny.billing.exception.PlusRequiredException;
import com.saveapenny.billing.exception.ReportHistoryLimitReachedException;
import com.saveapenny.billing.repository.BillingEntitlementRepository;
import com.saveapenny.budget.repository.BudgetRepository;
import com.saveapenny.config.TimeService;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.repository.GoalRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingAccessServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 19);

    @Mock
    private BillingEntitlementRepository billingEntitlementRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private TimeService timeService;

    @InjectMocks
    private BillingAccessServiceImpl billingAccessService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(timeService.now()).thenReturn(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private BillingEntitlement plusEntitlement(EntitlementStatus status) {
        return BillingEntitlement.builder()
                .userId(userId)
                .plan(Plan.PLUS)
                .status(status)
                .willRenew(true)
                .build();
    }

    @Test
    void requireFeature_throws_whenNoEntitlementExists() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(PlusRequiredException.class, () -> billingAccessService.requireFeature(userId, "assistant"));
    }

    @Test
    void requireFeature_throws_whenPlanIsFree() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.of(BillingEntitlement.builder()
                .userId(userId)
                .plan(Plan.FREE)
                .status(EntitlementStatus.INACTIVE)
                .build()));

        assertThrows(PlusRequiredException.class, () -> billingAccessService.requireFeature(userId, "assistant"));
    }

    @Test
    void requireFeature_throws_whenPlusButExpired() {
        when(billingEntitlementRepository.findById(userId))
                .thenReturn(Optional.of(plusEntitlement(EntitlementStatus.EXPIRED)));

        assertThrows(PlusRequiredException.class, () -> billingAccessService.requireFeature(userId, "assistant"));
    }

    @Test
    void requireFeature_passes_whenPlusActive() {
        when(billingEntitlementRepository.findById(userId))
                .thenReturn(Optional.of(plusEntitlement(EntitlementStatus.ACTIVE)));

        assertDoesNotThrow(() -> billingAccessService.requireFeature(userId, "assistant"));
    }

    @Test
    void requireFeature_passes_whenTrialing() {
        when(billingEntitlementRepository.findById(userId))
                .thenReturn(Optional.of(plusEntitlement(EntitlementStatus.TRIALING)));

        assertDoesNotThrow(() -> billingAccessService.requireFeature(userId, "assistant"));
    }

    @Test
    void requireFeature_passes_whenGracePeriod() {
        when(billingEntitlementRepository.findById(userId))
                .thenReturn(Optional.of(plusEntitlement(EntitlementStatus.GRACE_PERIOD)));

        assertDoesNotThrow(() -> billingAccessService.requireFeature(userId, "assistant"));
    }

    @Test
    void enforceBudgetCreationLimit_throws_whenFreeAndAtCap() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.empty());
        when(timeService.today()).thenReturn(TODAY);
        when(budgetRepository.countByUserIdAndEndDateGreaterThanEqual(userId, TODAY)).thenReturn(3L);

        assertThrows(FreePlanLimitReachedException.class, () -> billingAccessService.enforceBudgetCreationLimit(userId));
    }

    @Test
    void enforceBudgetCreationLimit_passes_whenFreeAndUnderCap() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.empty());
        when(timeService.today()).thenReturn(TODAY);
        when(budgetRepository.countByUserIdAndEndDateGreaterThanEqual(userId, TODAY)).thenReturn(2L);

        assertDoesNotThrow(() -> billingAccessService.enforceBudgetCreationLimit(userId));
    }

    @Test
    void enforceBudgetCreationLimit_passes_whenPlusEvenAtCap() {
        when(billingEntitlementRepository.findById(userId))
                .thenReturn(Optional.of(plusEntitlement(EntitlementStatus.ACTIVE)));

        assertDoesNotThrow(() -> billingAccessService.enforceBudgetCreationLimit(userId));
    }

    @Test
    void enforceGoalCreationLimit_throws_whenFreeAndAtCap() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.empty());
        when(goalRepository.countByUserIdAndStatusAndDeletedAtIsNull(userId, GoalStatus.ACTIVE)).thenReturn(1L);

        assertThrows(FreePlanLimitReachedException.class, () -> billingAccessService.enforceGoalCreationLimit(userId));
    }

    @Test
    void enforceGoalCreationLimit_passes_whenFreeAndUnderCap() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.empty());
        when(goalRepository.countByUserIdAndStatusAndDeletedAtIsNull(userId, GoalStatus.ACTIVE)).thenReturn(0L);

        assertDoesNotThrow(() -> billingAccessService.enforceGoalCreationLimit(userId));
    }

    @Test
    void enforceReportHistoryWindow_throws_whenFreeAndBeyondThreeMonths() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.empty());
        when(timeService.today()).thenReturn(TODAY);

        LocalDate from = TODAY.minusMonths(4);

        assertThrows(ReportHistoryLimitReachedException.class,
                () -> billingAccessService.enforceReportHistoryWindow(userId, from));
    }

    @Test
    void enforceReportHistoryWindow_passes_whenFreeAndWithinThreeMonths() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.empty());
        when(timeService.today()).thenReturn(TODAY);

        LocalDate from = TODAY.minusMonths(2);

        assertDoesNotThrow(() -> billingAccessService.enforceReportHistoryWindow(userId, from));
    }

    @Test
    void enforceReportHistoryWindow_passes_whenPlusRegardlessOfRange() {
        when(billingEntitlementRepository.findById(userId))
                .thenReturn(Optional.of(plusEntitlement(EntitlementStatus.ACTIVE)));

        assertDoesNotThrow(() -> billingAccessService.enforceReportHistoryWindow(userId, TODAY.minusYears(2)));
    }
}
