package com.saveapenny.billing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record RevenueCatSubscriber(
        @JsonProperty("original_app_user_id") String originalAppUserId,
        @JsonProperty("entitlements") Map<String, RevenueCatEntitlement> entitlements,
        @JsonProperty("subscriptions") Map<String, RevenueCatSubscription> subscriptions) {
}
