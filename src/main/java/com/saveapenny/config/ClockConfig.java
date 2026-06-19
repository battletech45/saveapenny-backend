package com.saveapenny.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Primary
    @Bean("systemClock")
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    public TimeService timeService(Clock systemClock) {
        return new TimeService(systemClock);
    }
}
