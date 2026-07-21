package com.saveapenny.billing.service;

import com.saveapenny.billing.entity.EntitlementStatus;
import com.saveapenny.billing.entity.Plan;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record ResolvedEntitlement(
        Plan plan,
        EntitlementStatus status,
        String store,
        String productId,
        String entitlementId,
        OffsetDateTime expiresAt,
        OffsetDateTime trialEndsAt,
        OffsetDateTime gracePeriodEndsAt,
        boolean willRenew) {
}
