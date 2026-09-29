package com.runway.order.controller;

import com.runway.order.service.ShipmentService;
import com.runway.order.util.ApiResponse;
import com.runway.order.util.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ShipmentController — REST API for Shipment tracking
 *
 * DEV MODE: No @PreAuthorize — all endpoints open without token.
 *
 * PRODUCTION: Add @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
 *   to updateStatus and assignTracking methods.
 */
@Slf4j
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Shipment tracking and management")
public class ShipmentController {

    private final ShipmentService shipmentService;

    /**
     * GET /api/shipments/order/{orderId}
     * Track shipment for a specific order.
     *
     * NOTE: Shipment only exists after order status is set to PROCESSING.
     * First use PUT /api/orders/admin/{id}/status with {"status":"PROCESSING"}
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Track shipment by order ID")
    public ResponseEntity<ApiResponse<ShipmentResponse>> trackShipment(
            @PathVariable Long orderId) {
        log.debug("GET /api/shipments/order/{}", orderId);
        ShipmentResponse shipment = shipmentService.trackByOrderId(orderId);
        return ResponseEntity.ok(
                ApiResponse.success("Shipment found", shipment));
    }

    /**
     * PUT /api/shipments/{id}/status
     * Update shipment status (Manager/Admin only in production).
     *
     * Request body:
     * { "status": "DISPATCHED" }
     *
     * Status flow:
     *   PROCESSING → DISPATCHED → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
     *
     * NOTE: When status = DELIVERED, order is automatically marked DELIVERED too.
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update shipment status — Manager/Admin")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShipmentStatusRequest request) {
        log.debug("PUT /api/shipments/{}/status — {}", id, request.getStatus());
        ShipmentResponse shipment = shipmentService.updateStatus(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Shipment status updated to " + shipment.getStatus(),
                        shipment));
    }

    /**
     * PUT /api/shipments/{id}/tracking
     * Assign carrier name and tracking number.
     *
     * Request body:
     * {
     *   "carrier": "BlueDart",
     *   "trackingNumber": "BD123456789IN"
     * }
     */
    @PutMapping("/{id}/tracking")
    @Operation(summary = "Assign tracking details — Manager/Admin")
    public ResponseEntity<ApiResponse<ShipmentResponse>> assignTracking(
            @PathVariable Long id,
            @Valid @RequestBody AssignTrackingRequest request) {
        log.debug("PUT /api/shipments/{}/tracking", id);
        ShipmentResponse shipment = shipmentService.assignTracking(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Tracking details assigned", shipment));
    }
}