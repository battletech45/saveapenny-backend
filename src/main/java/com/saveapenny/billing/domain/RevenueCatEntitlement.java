package com.saveapenny.billing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RevenueCatEntitlement(
        @JsonProperty("product_identifier") String productIdentifier,
        @JsonProperty("purchase_date") String purchaseDate,
        @JsonProperty("expires_date") String expiresDate,
        @JsonProperty("grace_period_expires_date") String gracePeriodExpiresDate,
        @JsonProperty("period_type") String periodType) {
}
