package com.runway.payment.client;

import com.runway.payment.client.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderServiceClient {

    // Calls GET /api/orders/{id}/internal — returns plain OrderResponse, no wrapper
    @GetMapping("/api/orders/{orderId}/internal")
    OrderDTO getOrderDetails(
            @PathVariable("orderId") Long orderId,
            @RequestHeader("X-User-Id") Long userId
    );

    // Calls PUT /api/orders/admin/{id}/status — already correct
    @PutMapping("/api/orders/admin/{orderId}/status")
    Object updateOrderStatusAsAdmin(
            @PathVariable("orderId") Long orderId,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> request
    );
}