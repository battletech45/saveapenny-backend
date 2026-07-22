package com.saveapenny.push.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

@Configuration
public class PushConfig {

    @Bean
    public RestClient pushRestClient(RestClient.Builder builder, PushProperties properties) {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofMillis(properties.timeoutMillis()))
                .withReadTimeout(Duration.ofMillis(properties.timeoutMillis()));
        return builder
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "push.fcm", name = "enabled", havingValue = "true")
    public FirebaseServiceAccount firebaseServiceAccount(
            PushProperties properties, ResourceLoader resourceLoader, ObjectMapper objectMapper) throws IOException {
        Resource resource = resourceLoader.getResource(properties.credentialsPath());
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, FirebaseServiceAccount.class);
        }
    }

    @Bean(name = "pushTaskExecutor")
    public TaskExecutor pushTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("push-worker-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.initialize();
        return executor;
    }
}
