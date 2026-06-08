package com.saveapenny.goal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GoalProgressProperties.class)
public class GoalConfig {
}
