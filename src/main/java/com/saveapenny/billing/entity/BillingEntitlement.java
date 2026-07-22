package com.saveapenny.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "billing_entitlement",
        indexes = {@Index(name = "idx_billing_entitlement_status", columnList = "status")})
public class BillingEntitlement {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntitlementStatus status;

    @Column(length = 20)
    private String store;

    @Column(name = "product_id", length = 100)
    private String productId;

    @Column(name = "entitlement_id", length = 100)
    private String entitlementId;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "trial_ends_at")
    private OffsetDateTime trialEndsAt;

    @Column(name = "grace_period_ends_at")
    private OffsetDateTime gracePeriodEndsAt;

    @Column(name = "will_renew", nullable = false)
    private boolean willRenew;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (plan == null) {
            plan = Plan.FREE;
        }
        if (status == null) {
            status = EntitlementStatus.INACTIVE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Locally downgrades a stale cached status when time has passed the known expiry without a
     * fresh RevenueCat sync (the client hasn't called /billing/sync recently).
     * Never upgrades status — renewals/reactivations can only be learned from RevenueCat itself.
     */
    public EntitlementStatus effectiveStatus(OffsetDateTime now) {
        if ((status == EntitlementStatus.ACTIVE || status == EntitlementStatus.TRIALING)
                && expiresAt != null
                && expiresAt.isBefore(now)) {
            return gracePeriodEndsAt != null && gracePeriodEndsAt.isAfter(now)
                    ? EntitlementStatus.GRACE_PERIOD
                    : EntitlementStatus.EXPIRED;
        }
        if (status == EntitlementStatus.GRACE_PERIOD && gracePeriodEndsAt != null && !gracePeriodEndsAt.isAfter(now)) {
            return EntitlementStatus.EXPIRED;
        }
        return status;
    }
}
