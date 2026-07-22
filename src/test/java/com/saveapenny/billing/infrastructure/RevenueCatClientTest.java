package com.saveapenny.billing.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.saveapenny.billing.config.RevenueCatProperties;
import com.saveapenny.billing.domain.RevenueCatSubscriberResponse;
import com.saveapenny.billing.exception.RevenueCatAuthenticationException;
import com.saveapenny.billing.exception.RevenueCatClientException;
import com.saveapenny.billing.exception.RevenueCatDisabledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

class RevenueCatClientTest {

    private MockRestServiceServer mockServer;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.revenuecat.example/v1");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private RevenueCatClient clientWith(RevenueCatProperties properties) {
        return new RevenueCatClient(restClient, properties);
    }

    @Test
    void fetchSubscriber_returnsNull_whenRevenueCatReturns404() {
        RevenueCatProperties properties = new RevenueCatProperties(true, "secret", "https://api.revenuecat.example/v1", "plus");
        mockServer.expect(requestTo("https://api.revenuecat.example/v1/subscribers/user-1"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{}").contentType(MediaType.APPLICATION_JSON));

        RevenueCatSubscriberResponse response = clientWith(properties).fetchSubscriber("user-1");

        assertNull(response);
    }

    @Test
    void fetchSubscriber_throwsAuthenticationException_when401() {
        RevenueCatProperties properties = new RevenueCatProperties(true, "bad-secret", "https://api.revenuecat.example/v1", "plus");
        mockServer.expect(requestTo("https://api.revenuecat.example/v1/subscribers/user-1"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{}").contentType(MediaType.APPLICATION_JSON));

        assertThrows(RevenueCatAuthenticationException.class, () -> clientWith(properties).fetchSubscriber("user-1"));
    }

    @Test
    void fetchSubscriber_throwsAuthenticationException_when403() {
        RevenueCatProperties properties = new RevenueCatProperties(true, "secret", "https://api.revenuecat.example/v1", "plus");
        mockServer.expect(requestTo("https://api.revenuecat.example/v1/subscribers/user-1"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("{}").contentType(MediaType.APPLICATION_JSON));

        assertThrows(RevenueCatAuthenticationException.class, () -> clientWith(properties).fetchSubscriber("user-1"));
    }

    @Test
    void fetchSubscriber_throwsClientException_on5xx() {
        RevenueCatProperties properties = new RevenueCatProperties(true, "secret", "https://api.revenuecat.example/v1", "plus");
        mockServer.expect(requestTo("https://api.revenuecat.example/v1/subscribers/user-1"))
                .andRespond(withServerError());

        assertThrows(RevenueCatClientException.class, () -> clientWith(properties).fetchSubscriber("user-1"));
    }

    @Test
    void fetchSubscriber_throwsDisabledException_whenNotEnabled() {
        RevenueCatProperties properties = new RevenueCatProperties(false, "secret", "https://api.revenuecat.example/v1", "plus");

        assertThrows(RevenueCatDisabledException.class, () -> clientWith(properties).fetchSubscriber("user-1"));
    }

    @Test
    void fetchSubscriber_throwsDisabledException_whenSecretMissing() {
        RevenueCatProperties properties = new RevenueCatProperties(true, "", "https://api.revenuecat.example/v1", "plus");

        assertThrows(RevenueCatDisabledException.class, () -> clientWith(properties).fetchSubscriber("user-1"));
    }

    @Test
    void fetchSubscriber_returnsBody_onSuccess() {
        RevenueCatProperties properties = new RevenueCatProperties(true, "secret", "https://api.revenuecat.example/v1", "plus");
        mockServer.expect(requestTo("https://api.revenuecat.example/v1/subscribers/user-1"))
                .andRespond(withSuccess("""
                        {
                          "subscriber": {
                            "original_app_user_id": "user-1",
                            "entitlements": {},
                            "subscriptions": {}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        RevenueCatSubscriberResponse response = clientWith(properties).fetchSubscriber("user-1");

        assertNotNull(response);
        assertNotNull(response.subscriber());
        assertEquals("user-1", response.subscriber().originalAppUserId());
    }
}
