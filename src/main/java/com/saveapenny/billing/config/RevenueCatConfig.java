package com.saveapenny.billing.config;

import com.saveapenny.billing.infrastructure.RevenueCatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class RevenueCatConfig {

    @Bean
    public RestClient revenueCatRestClient(RestClient.Builder builder, RevenueCatProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.secretApiKey())
                .build();
    }

    @Bean
    public RevenueCatClient revenueCatClient(RestClient revenueCatRestClient, RevenueCatProperties properties) {
        return new RevenueCatClient(revenueCatRestClient, properties);
    }
}
