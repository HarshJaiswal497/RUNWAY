package com.runway.engagement.client.order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderClient {
    @GetMapping("/api/orders/exists")
    Boolean hasUserPurchasedProduct(@RequestParam("userId") Long userId, @RequestParam("productId") Long productId, @RequestParam("status") String status);
}