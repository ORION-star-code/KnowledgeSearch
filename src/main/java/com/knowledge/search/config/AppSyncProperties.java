package com.knowledge.search.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sync")
public record AppSyncProperties(
        Full full,
        Retry retry
) {
    public record Full(int pageSize, boolean enabled) {
    }

    public record Retry(int maxAttempts, Duration initialDelay, Duration scanInterval, boolean enabled) {
    }
}
