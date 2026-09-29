package com.runway.order.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "CUSTOMER-ENGAGEMENT-SERVICE")
public interface EngagementServiceClient {

    @PostMapping("/internal/notify")
    void notify(@RequestBody NotifyRequest request);

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class NotifyRequest {
        private String event;
        private Long userId;
        private Map<String, Object> payload;
    }
}
