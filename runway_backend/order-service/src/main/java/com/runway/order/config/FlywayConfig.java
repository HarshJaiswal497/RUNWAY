package com.runway.order.config;

import org.springframework.context.annotation.Configuration;

/**
 * FlywayConfig
 * Spring Boot auto-configures Flyway from application.properties.
 * Migration files: src/main/resources/db/migration/V*.sql
 */
@Configuration
public class FlywayConfig {
    // Auto-configured via spring.flyway.* properties
}
