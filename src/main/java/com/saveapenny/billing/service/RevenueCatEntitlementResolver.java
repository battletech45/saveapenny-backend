package com.saveapenny.billing.service;

import com.saveapenny.billing.config.RevenueCatProperties;
import com.saveapenny.billing.domain.RevenueCatEntitlement;
import com.saveapenny.billing.domain.RevenueCatSubscriber;
import com.saveapenny.billing.domain.RevenueCatSubscription;
import com.saveapenny.billing.entity.EntitlementStatus;
import com.saveapenny.billing.entity.Plan;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RevenueCatEntitlementResolver {

    private final RevenueCatProperties properties;

    public RevenueCatEntitlementResolver(RevenueCatProperties properties) {
        this.properties = properties;
    }

    public ResolvedEntitlement resolve(RevenueCatSubscriber subscriber) {
        RevenueCatEntitlement entitlement = subscriber == null || subscriber.entitlements() == null
                ? null
                : subscriber.entitlements().get(properties.entitlementIdentifier());

        if (entitlement == null) {
            return ResolvedEntitlement.builder()
                    .plan(Plan.FREE)
                    .status(EntitlementStatus.INACTIVE)
                    .willRenew(false)
                    .build();
        }

        OffsetDateTime expiresAt = parse(entitlement.expiresDate());
        OffsetDateTime now = OffsetDateTime.now();

        RevenueCatSubscription subscription = findSubscription(subscriber.subscriptions(), entitlement.productIdentifier());
        OffsetDateTime gracePeriodEndsAt = parse(entitlement.gracePeriodExpiresDate());

        EntitlementStatus status = resolveStatus(entitlement, expiresAt, gracePeriodEndsAt, now);
        boolean willRenew = resolveWillRenew(subscription, status);
        OffsetDateTime trialEndsAt = "TRIAL".equalsIgnoreCase(entitlement.periodType()) ? expiresAt : null;

        return ResolvedEntitlement.builder()
                .plan(status == EntitlementStatus.INACTIVE || status == EntitlementStatus.EXPIRED ? Plan.FREE : Plan.PLUS)
                .status(status)
                .store(subscription == null ? null : subscription.store())
                .productId(entitlement.productIdentifier())
                .entitlementId(properties.entitlementIdentifier())
                .expiresAt(expiresAt)
                .trialEndsAt(trialEndsAt)
                .gracePeriodEndsAt(gracePeriodEndsAt)
                .willRenew(willRenew)
                .build();
    }

    private EntitlementStatus resolveStatus(
            RevenueCatEntitlement entitlement, OffsetDateTime expiresAt, OffsetDateTime gracePeriodEndsAt, OffsetDateTime now) {
        if (expiresAt == null || expiresAt.isAfter(now)) {
            return "TRIAL".equalsIgnoreCase(entitlement.periodType()) ? EntitlementStatus.TRIALING : EntitlementStatus.ACTIVE;
        }
        if (gracePeriodEndsAt != null && gracePeriodEndsAt.isAfter(now)) {
            return EntitlementStatus.GRACE_PERIOD;
        }
        return EntitlementStatus.EXPIRED;
    }

    private boolean resolveWillRenew(RevenueCatSubscription subscription, EntitlementStatus status) {
        if (status == EntitlementStatus.EXPIRED || status == EntitlementStatus.INACTIVE) {
            return false;
        }
        if (subscription == null) {
            return false;
        }
        return !StringUtils.hasText(subscription.unsubscribeDetectedAt())
                && !StringUtils.hasText(subscription.billingIssuesDetectedAt());
    }

    private RevenueCatSubscription findSubscription(Map<String, RevenueCatSubscription> subscriptions, String productId) {
        if (subscriptions == null || productId == null) {
            return null;
        }
        return subscriptions.get(productId);
    }

    private OffsetDateTime parse(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return OffsetDateTime.parse(value);
    }
}
