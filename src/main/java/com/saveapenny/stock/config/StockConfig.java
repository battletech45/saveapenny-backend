package com.saveapenny.stock.config;

import com.saveapenny.config.TimeService;
import com.saveapenny.stock.infrastructure.AlphaVantageClient;
import com.saveapenny.stock.infrastructure.RateLimitTracker;
import com.saveapenny.stock.service.StockService;
import com.saveapenny.stock.service.impl.StockServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class StockConfig {

    @Bean
    public RestClient stockRestClient(RestClient.Builder builder, StockProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RateLimitTracker rateLimitTracker(StockProperties properties, TimeService timeService) {
        return new RateLimitTracker(properties.rateLimitPerMinute(), properties.rateLimitPerDay(), timeService);
    }

    @Bean
    public AlphaVantageClient alphaVantageClient(RestClient stockRestClient, StockProperties properties, RateLimitTracker rateLimitTracker) {
        return new AlphaVantageClient(stockRestClient, properties, rateLimitTracker);
    }

    @Bean
    public StockService stockService(AlphaVantageClient alphaVantageClient, StockProperties properties, MeterRegistry meterRegistry) {
        return new StockServiceImpl(alphaVantageClient, properties, meterRegistry);
    }
}
