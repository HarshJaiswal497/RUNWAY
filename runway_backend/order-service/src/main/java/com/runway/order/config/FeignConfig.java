package com.runway.order.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * FeignConfig — Forwards authentication headers from incoming requests
 * to outgoing Feign calls.
 *
 * When Order Service calls Catalog Service via Feign,
 * the X-User-Id and X-User-Role headers (set by API Gateway)
 * are automatically forwarded.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // Forward auth headers set by API Gateway
                String userId = request.getHeader("X-User-Id");
                String userRole = request.getHeader("X-User-Role");
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                }
                if (userRole != null) {
                    requestTemplate.header("X-USER-ROLE", userRole);
                }
            }
        };
    }
}
