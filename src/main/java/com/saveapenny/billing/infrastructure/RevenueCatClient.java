package com.saveapenny.billing.infrastructure;

import com.saveapenny.billing.config.RevenueCatProperties;
import com.saveapenny.billing.domain.RevenueCatSubscriberResponse;
import com.saveapenny.billing.exception.RevenueCatClientException;
import com.saveapenny.billing.exception.RevenueCatDisabledException;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

public class RevenueCatClient {

    private final RestClient restClient;
    private final RevenueCatProperties properties;

    public RevenueCatClient(RestClient restClient, RevenueCatProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public RevenueCatSubscriberResponse fetchSubscriber(String appUserId) {
        if (!properties.enabled() || !StringUtils.hasText(properties.secretApiKey())) {
            throw new RevenueCatDisabledException(
                    "RevenueCat integration is disabled. Set REVENUECAT_ENABLED and REVENUECAT_SECRET_API_KEY.");
        }

        try {
            return restClient.get()
                    .uri("/subscribers/{appUserId}", appUserId)
                    .retrieve()
                    .body(RevenueCatSubscriberResponse.class);
        } catch (Exception e) {
            throw new RevenueCatClientException(
                    "Failed to fetch subscriber from RevenueCat: " + e.getMessage(), e);
        }
    }
}
