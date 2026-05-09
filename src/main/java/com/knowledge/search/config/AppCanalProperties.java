package com.knowledge.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.canal")
public record AppCanalProperties(boolean enabled, String destination, String server, String username, String password) {
}
