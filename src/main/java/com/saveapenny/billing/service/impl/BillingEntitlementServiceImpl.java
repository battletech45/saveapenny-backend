package com.saveapenny.billing.service.impl;

import com.saveapenny.analytics.dto.AnalyticsEvent;
import com.saveapenny.analytics.service.AnalyticsEventPublisher;
import com.saveapenny.billing.domain.RevenueCatSubscriberResponse;
import com.saveapenny.billing.dto.EntitlementLimitsResponse;
import com.saveapenny.billing.dto.EntitlementResponse;
import com.saveapenny.billing.dto.FeatureAccessResponse;
import com.saveapenny.billing.entity.BillingCustomer;
import com.saveapenny.billing.entity.BillingEntitlement;
import com.saveapenny.billing.entity.EntitlementStatus;
import com.saveapenny.billing.entity.Plan;
import com.saveapenny.billing.infrastructure.RevenueCatClient;
import com.saveapenny.billing.repository.BillingCustomerRepository;
import com.saveapenny.billing.repository.BillingEntitlementRepository;
import com.saveapenny.billing.service.BillingEntitlementService;
import com.saveapenny.billing.service.PlanCapabilities;
import com.saveapenny.billing.service.ResolvedEntitlement;
import com.saveapenny.billing.service.RevenueCatEntitlementResolver;
import com.saveapenny.budget.repository.BudgetRepository;
import com.saveapenny.config.TimeService;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.repository.GoalRepository;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingEntitlementServiceImpl implements BillingEntitlementService {

    private final BillingCustomerRepository billingCustomerRepository;
    private final BillingEntitlementRepository billingEntitlementRepository;
    private final RevenueCatClient revenueCatClient;
    private final RevenueCatEntitlementResolver resolver;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final TimeService timeService;
    private final AnalyticsEventPublisher analyticsEventPublisher;

    public BillingEntitlementServiceImpl(
            BillingCustomerRepository billingCustomerRepository,
            BillingEntitlementRepository billingEntitlementRepository,
            RevenueCatClient revenueCatClient,
            RevenueCatEntitlementResolver resolver,
            BudgetRepository budgetRepository,
            GoalRepository goalRepository,
            TimeService timeService,
            AnalyticsEventPublisher analyticsEventPublisher) {
        this.billingCustomerRepository = billingCustomerRepository;
        this.billingEntitlementRepository = billingEntitlementRepository;
        this.revenueCatClient = revenueCatClient;
        this.resolver = resolver;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.timeService = timeService;
        this.analyticsEventPublisher = analyticsEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public EntitlementResponse getEntitlement(UUID userId) {
        BillingEntitlement entity = billingEntitlementRepository.findById(userId).orElse(null);
        return toResponse(userId, entity);
    }

    @Override
    @Transactional
    public EntitlementResponse sync(UUID userId) {
        BillingEntitlement previous = billingEntitlementRepository.findById(userId).orElse(null);
        BillingEntitlement updated = refreshFromRevenueCat(userId);
        emitPurchaseSignal(previous, updated);
        return toResponse(userId, updated);
    }

    private BillingEntitlement refreshFromRevenueCat(UUID userId) {
        BillingCustomer customer = billingCustomerRepository.findById(userId)
                .orElseGet(() -> billingCustomerRepository.save(BillingCustomer.builder()
                        .userId(userId)
                        .revenuecatAppUserId(userId.toString())
                        .build()));

        RevenueCatSubscriberResponse response = revenueCatClient.fetchSubscriber(customer.getRevenuecatAppUserId());
        ResolvedEntitlement resolved = resolver.resolve(response == null ? null : response.subscriber());

        BillingEntitlement entity = billingEntitlementRepository.findById(userId)
                .orElseGet(() -> BillingEntitlement.builder().userId(userId).build());
        entity.setPlan(resolved.plan());
        entity.setStatus(resolved.status());
        entity.setStore(resolved.store());
        entity.setProductId(resolved.productId());
        entity.setEntitlementId(resolved.entitlementId());
        entity.setExpiresAt(resolved.expiresAt());
        entity.setTrialEndsAt(resolved.trialEndsAt());
        entity.setGracePeriodEndsAt(resolved.gracePeriodEndsAt());
        entity.setWillRenew(resolved.willRenew());
        entity.setLastSyncedAt(timeService.now().atOffset(java.time.ZoneOffset.UTC));

        return billingEntitlementRepository.save(entity);
    }

    private void emitPurchaseSignal(BillingEntitlement previous, BillingEntitlement updated) {
        boolean wasActive = previous != null && isActive(previous.getStatus());
        boolean isActiveNow = isActive(updated.getStatus());
        if (!wasActive && isActiveNow) {
            String eventName = updated.getStatus() == EntitlementStatus.TRIALING ? "trial_started" : "subscription_started";
            analyticsEventPublisher.publish(new AnalyticsEvent(eventName, Map.of("plan", updated.getPlan().name())));
        }
    }

    private boolean isActive(EntitlementStatus status) {
        return status == EntitlementStatus.ACTIVE
                || status == EntitlementStatus.TRIALING
                || status == EntitlementStatus.GRACE_PERIOD;
    }

    private EntitlementResponse toResponse(UUID userId, BillingEntitlement entity) {
        EntitlementStatus status = entity == null
                ? EntitlementStatus.INACTIVE
                : entity.effectiveStatus(timeService.now().atOffset(java.time.ZoneOffset.UTC));
        // A lazily-detected expiry (no fresh /sync call yet) must not leave PLUS features unlocked.
        Plan plan = entity == null || !isActive(status) ? Plan.FREE : entity.getPlan();

        long activeBudgets = budgetRepository.countByUserIdAndEndDateGreaterThanEqual(userId, timeService.today());
        long activeGoals = goalRepository.countByUserIdAndStatusAndDeletedAtIsNull(userId, GoalStatus.ACTIVE);

        FeatureAccessResponse features = PlanCapabilities.featuresFor(plan);
        EntitlementLimitsResponse limits = EntitlementLimitsResponse.builder()
                .activeBudgets(activeBudgets)
                .maxActiveBudgets(PlanCapabilities.maxActiveBudgets(plan))
                .activeGoals(activeGoals)
                .maxActiveGoals(PlanCapabilities.maxActiveGoals(plan))
                .reportHistoryMonths(PlanCapabilities.reportHistoryMonths(plan))
                .build();

        return EntitlementResponse.builder()
                .plan(plan.name().toLowerCase(Locale.ROOT))
                .status(status.name().toLowerCase(Locale.ROOT))
                .active(isActive(status))
                .willRenew(entity != null && entity.isWillRenew())
                .expiresAt(entity == null ? null : entity.getExpiresAt())
                .trialEndsAt(entity == null ? null : entity.getTrialEndsAt())
                .features(features)
                .limits(limits)
                .build();
    }
}
