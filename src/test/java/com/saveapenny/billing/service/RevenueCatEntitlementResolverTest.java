package com.saveapenny.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.billing.config.RevenueCatProperties;
import com.saveapenny.billing.domain.RevenueCatEntitlement;
import com.saveapenny.billing.domain.RevenueCatSubscriber;
import com.saveapenny.billing.domain.RevenueCatSubscription;
import com.saveapenny.billing.entity.EntitlementStatus;
import com.saveapenny.billing.entity.Plan;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RevenueCatEntitlementResolverTest {

    private final RevenueCatProperties properties =
            new RevenueCatProperties(true, "secret", "https://api.revenuecat.com/v1", "plus");
    private final RevenueCatEntitlementResolver resolver = new RevenueCatEntitlementResolver(properties);

    @Test
    void resolve_returnsFreeInactive_whenSubscriberIsNull() {
        ResolvedEntitlement result = resolver.resolve(null);

        assertEquals(Plan.FREE, result.plan());
        assertEquals(EntitlementStatus.INACTIVE, result.status());
        assertFalse(result.willRenew());
    }

    @Test
    void resolve_returnsFreeInactive_whenEntitlementMissing() {
        RevenueCatSubscriber subscriber = new RevenueCatSubscriber("user-1", Map.of(), Map.of());

        ResolvedEntitlement result = resolver.resolve(subscriber);

        assertEquals(Plan.FREE, result.plan());
        assertEquals(EntitlementStatus.INACTIVE, result.status());
    }

    @Test
    void resolve_returnsActivePlus_whenEntitlementNotExpired() {
        String futureDate = OffsetDateTime.now().plusDays(30).toString();
        RevenueCatEntitlement entitlement =
                new RevenueCatEntitlement("plus_monthly", OffsetDateTime.now().minusDays(1).toString(), futureDate, null, "NORMAL");
        RevenueCatSubscription subscription = new RevenueCatSubscription("APP_STORE", null, futureDate, null, null);
        RevenueCatSubscriber subscriber =
                new RevenueCatSubscriber("user-1", Map.of("plus", entitlement), Map.of("plus_monthly", subscription));

        ResolvedEntitlement result = resolver.resolve(subscriber);

        assertEquals(Plan.PLUS, result.plan());
        assertEquals(EntitlementStatus.ACTIVE, result.status());
        assertTrue(result.willRenew());
        assertEquals("APP_STORE", result.store());
        assertEquals("plus_monthly", result.productId());
        assertNull(result.trialEndsAt());
    }

    @Test
    void resolve_returnsTrialing_whenPeriodTypeIsTrial() {
        String futureDate = OffsetDateTime.now().plusDays(7).toString();
        RevenueCatEntitlement entitlement =
                new RevenueCatEntitlement("plus_monthly", OffsetDateTime.now().toString(), futureDate, null, "TRIAL");
        RevenueCatSubscriber subscriber = new RevenueCatSubscriber("user-1", Map.of("plus", entitlement), Map.of());

        ResolvedEntitlement result = resolver.resolve(subscriber);

        assertEquals(EntitlementStatus.TRIALING, result.status());
        assertEquals(futureDate, result.trialEndsAt().toString());
    }

    @Test
    void resolve_returnsGracePeriod_whenExpiredButGraceWindowActive() {
        String pastDate = OffsetDateTime.now().minusDays(1).toString();
        String futureGraceDate = OffsetDateTime.now().plusDays(3).toString();
        RevenueCatEntitlement entitlement =
                new RevenueCatEntitlement("plus_monthly", OffsetDateTime.now().minusDays(31).toString(), pastDate, futureGraceDate, "NORMAL");
        RevenueCatSubscriber subscriber = new RevenueCatSubscriber("user-1", Map.of("plus", entitlement), Map.of());

        ResolvedEntitlement result = resolver.resolve(subscriber);

        assertEquals(EntitlementStatus.GRACE_PERIOD, result.status());
        assertEquals(Plan.PLUS, result.plan());
    }

    @Test
    void resolve_returnsExpiredFree_whenPastExpiryAndNoGrace() {
        String pastDate = OffsetDateTime.now().minusDays(5).toString();
        RevenueCatEntitlement entitlement =
                new RevenueCatEntitlement("plus_monthly", OffsetDateTime.now().minusDays(35).toString(), pastDate, null, "NORMAL");
        RevenueCatSubscriber subscriber = new RevenueCatSubscriber("user-1", Map.of("plus", entitlement), Map.of());

        ResolvedEntitlement result = resolver.resolve(subscriber);

        assertEquals(EntitlementStatus.EXPIRED, result.status());
        assertEquals(Plan.FREE, result.plan());
        assertFalse(result.willRenew());
    }

    @Test
    void resolve_willRenewFalse_whenUnsubscribeDetected() {
        String futureDate = OffsetDateTime.now().plusDays(10).toString();
        RevenueCatEntitlement entitlement =
                new RevenueCatEntitlement("plus_monthly", OffsetDateTime.now().toString(), futureDate, null, "NORMAL");
        RevenueCatSubscription subscription =
                new RevenueCatSubscription("APP_STORE", null, futureDate, OffsetDateTime.now().toString(), null);
        RevenueCatSubscriber subscriber =
                new RevenueCatSubscriber("user-1", Map.of("plus", entitlement), Map.of("plus_monthly", subscription));

        ResolvedEntitlement result = resolver.resolve(subscriber);

        assertEquals(EntitlementStatus.ACTIVE, result.status());
        assertFalse(result.willRenew());
    }
}
