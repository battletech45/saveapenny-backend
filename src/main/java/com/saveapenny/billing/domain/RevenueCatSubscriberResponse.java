package com.saveapenny.billing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RevenueCatSubscriberResponse(@JsonProperty("subscriber") RevenueCatSubscriber subscriber) {
}
