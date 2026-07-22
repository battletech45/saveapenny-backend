package com.saveapenny.billing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RevenueCatSubscription(
        @JsonProperty("store") String store,
        @JsonProperty("purchase_date") String purchaseDate,
        @JsonProperty("expires_date") String expiresDate,
        @JsonProperty("unsubscribe_detected_at") String unsubscribeDetectedAt,
        @JsonProperty("billing_issues_detected_at") String billingIssuesDetectedAt) {
}
