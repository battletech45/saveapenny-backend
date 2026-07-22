package com.saveapenny.billing.infrastructure;

import com.saveapenny.billing.config.RevenueCatProperties;
import com.saveapenny.billing.domain.RevenueCatSubscriberResponse;
import com.saveapenny.billing.exception.RevenueCatAuthenticationException;
import com.saveapenny.billing.exception.RevenueCatClientException;
import com.saveapenny.billing.exception.RevenueCatDisabledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class RevenueCatClient {

    private static final Logger log = LoggerFactory.getLogger(RevenueCatClient.class);

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
        } catch (HttpClientErrorException.NotFound e) {
            // RevenueCat returns 404 for an app_user_id it has never seen (e.g. a brand-new
            // user who hasn't made a purchase yet) - this is a normal "no entitlement" case,
            // not a provider failure, so callers should see no subscriber rather than an error.
            return null;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.error("RevenueCat rejected the configured secret API key (HTTP {})", e.getStatusCode().value(), e);
            throw new RevenueCatAuthenticationException(
                    "RevenueCat rejected the configured API key (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (RestClientException e) {
            log.warn("Failed to fetch subscriber {} from RevenueCat", appUserId, e);
            throw new RevenueCatClientException(
                    "Failed to fetch subscriber from RevenueCat: " + e.getMessage(), e);
        }
    }
}
