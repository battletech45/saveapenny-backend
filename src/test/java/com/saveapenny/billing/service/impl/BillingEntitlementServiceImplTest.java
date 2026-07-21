package com.saveapenny.billing.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.saveapenny.analytics.service.AnalyticsEventPublisher;
import com.saveapenny.billing.dto.EntitlementResponse;
import com.saveapenny.billing.entity.BillingEntitlement;
import com.saveapenny.billing.entity.EntitlementStatus;
import com.saveapenny.billing.entity.Plan;
import com.saveapenny.billing.infrastructure.RevenueCatClient;
import com.saveapenny.billing.repository.BillingCustomerRepository;
import com.saveapenny.billing.repository.BillingEntitlementRepository;
import com.saveapenny.billing.service.RevenueCatEntitlementResolver;
import com.saveapenny.budget.repository.BudgetRepository;
import com.saveapenny.config.TimeService;
import com.saveapenny.goal.repository.GoalRepository;
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

/**
 * Regression coverage for a bug where {@code toResponse()} serialized
 * {@code plan.name()} / {@code status.name()} directly, producing uppercase
 * wire values ("PLUS", "GRACE_PERIOD") instead of the lowercase values the
 * Flutter client's {@code Plan.fromWire} / {@code EntitlementStatus.fromWire}
 * expect. Both client enums silently fall back to free/inactive on a
 * mismatch rather than throwing, so the bug was invisible without a test
 * asserting the exact wire casing.
 */
@ExtendWith(MockitoExtension.class)
class BillingEntitlementServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 19);

    @Mock
    private BillingCustomerRepository billingCustomerRepository;
    @Mock
    private BillingEntitlementRepository billingEntitlementRepository;
    @Mock
    private RevenueCatClient revenueCatClient;
    @Mock
    private RevenueCatEntitlementResolver resolver;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private TimeService timeService;
    @Mock
    private AnalyticsEventPublisher analyticsEventPublisher;

    @InjectMocks
    private BillingEntitlementServiceImpl billingEntitlementService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        org.mockito.Mockito.lenient()
                .when(timeService.now())
                .thenReturn(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    @Test
    void getEntitlement_serializesPlanAndStatusInLowercase_whenEntitlementIsPlusAndGracePeriod() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.of(BillingEntitlement.builder()
                .userId(userId)
                .plan(Plan.PLUS)
                .status(EntitlementStatus.GRACE_PERIOD)
                .willRenew(true)
                .build()));
        when(timeService.today()).thenReturn(TODAY);
        when(budgetRepository.countByUserIdAndEndDateGreaterThanEqual(userId, TODAY)).thenReturn(0L);
        when(goalRepository.countByUserIdAndStatusAndDeletedAtIsNull(userId, com.saveapenny.goal.entity.GoalStatus.ACTIVE))
                .thenReturn(0L);

        EntitlementResponse response = billingEntitlementService.getEntitlement(userId);

        assertEquals("plus", response.getPlan());
        assertEquals("grace_period", response.getStatus());
    }

    @Test
    void getEntitlement_defaultsToLowercaseFreeAndInactive_whenNoEntitlementExists() {
        when(billingEntitlementRepository.findById(userId)).thenReturn(Optional.empty());
        when(timeService.today()).thenReturn(TODAY);
        when(budgetRepository.countByUserIdAndEndDateGreaterThanEqual(userId, TODAY)).thenReturn(0L);
        when(goalRepository.countByUserIdAndStatusAndDeletedAtIsNull(userId, com.saveapenny.goal.entity.GoalStatus.ACTIVE))
                .thenReturn(0L);

        EntitlementResponse response = billingEntitlementService.getEntitlement(userId);

        assertEquals("free", response.getPlan());
        assertEquals("inactive", response.getStatus());
    }
}
