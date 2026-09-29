package com.runway.catalog.controller;

import com.runway.catalog.dto.response.InventoryResponse;
import com.runway.catalog.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/catalog/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // Anyone can check stock (Customer, Seller, Admin)
    @GetMapping("/{productId}")
    public ResponseEntity<Integer> getStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getStockForProduct(productId));
    }

    // Only Owner or Admin can update stock
    @PatchMapping("/{productId}/update")
    public ResponseEntity<InventoryResponse> updateStock(
            @PathVariable Long productId,
            @RequestParam Integer amount,
            @RequestHeader("X-User-Id") Long userIdStr,      // Injected by Gateway
            @RequestHeader("X-User-Role") String role) {  // Injected by Gateway

        System.out.println(">>> RECEIVED ID: " + userIdStr);
        System.out.println(">>> RECEIVED ROLE: " + role);

        if (userIdStr == null) {
            throw new RuntimeException("Gateway is NOT passing the headers!");
        }

        Long userId = Long.parseLong(String.valueOf(userIdStr));

        inventoryService.updateStock(productId, amount, userId, role);

        Integer newQty = inventoryService.getStockForProduct(productId);
        return ResponseEntity.ok(new InventoryResponse(
                productId,
                newQty,
                "SUCCESS",
                "Stock adjusted successfully"
        ));
    }
}