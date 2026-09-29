package com.runway.order.service;

import com.runway.order.client.EngagementServiceClient;
import com.runway.order.exception.ResourceNotFoundException;
import com.runway.order.model.Order;
import com.runway.order.model.Shipment;
import com.runway.order.repository.OrderRepository;
import com.runway.order.repository.ShipmentRepository;
import com.runway.order.util.Dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository    orderRepository;
    private final EngagementServiceClient engagementClient;

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse trackByOrderId(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Shipment not found for order: " + orderId));
        return ShipmentResponse.from(shipment);
    }

    @Override
    @Transactional
    public ShipmentResponse updateStatus(Long shipmentId,
                                         UpdateShipmentStatusRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Shipment", shipmentId));

        Shipment.ShipmentStatus oldStatus = shipment.getStatus();
        shipment.setStatus(request.getStatus());

        // Set timestamps based on status transitions
        if (request.getStatus() == Shipment.ShipmentStatus.DISPATCHED
                && shipment.getShippedAt() == null) {
            shipment.setShippedAt(LocalDateTime.now());
        }
        if (request.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(LocalDateTime.now());
            // Also update the order status to DELIVERED
            updateOrderToDelivered(shipment.getOrder().getId());
        }

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipment #{} status: {} → {}",
            shipmentId, oldStatus, request.getStatus());

        Long userId = shipment.getOrder().getUserId();
        String orderNumber = shipment.getOrder().getOrderNumber();

        if (request.getStatus() == Shipment.ShipmentStatus.DISPATCHED) {
            String carrier = (shipment.getCarrier() != null && !shipment.getCarrier().isBlank())
                ? shipment.getCarrier() : "Pending";
            String trackingNumber = (shipment.getTrackingNumber() != null && !shipment.getTrackingNumber().isBlank())
                ? shipment.getTrackingNumber() : "Pending";
            notifyEngagement("SHIPMENT_DISPATCHED", userId, Map.of(
                "orderNumber", orderNumber,
                "carrier", carrier,
                "trackingNumber", trackingNumber
            ));
            notifyEngagement("ORDER_SHIPPED", userId, Map.of("orderNumber", orderNumber));
        } else if (request.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
            String deliveredAt = shipment.getDeliveredAt() != null
                ? shipment.getDeliveredAt().toLocalDate().toString() : "today";
            notifyEngagement("SHIPMENT_DELIVERED", userId, Map.of(
                "orderNumber", orderNumber,
                "deliveredAt", deliveredAt
            ));
            notifyEngagement("ORDER_DELIVERED", userId, Map.of("orderNumber", orderNumber));
        }

        return ShipmentResponse.from(saved);
    }

    @Override
    @Transactional
    public ShipmentResponse assignTracking(Long shipmentId,
                                            AssignTrackingRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Shipment", shipmentId));

        shipment.setCarrier(request.getCarrier());
        shipment.setTrackingNumber(request.getTrackingNumber());

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Tracking assigned to shipment #{}: {} / {}",
            shipmentId, request.getCarrier(), request.getTrackingNumber());

        return ShipmentResponse.from(saved);
    }

    private void notifyEngagement(String event, Long userId, Map<String, Object> payload) {
        try {
            engagementClient.notify(new EngagementServiceClient.NotifyRequest(event, userId, payload));
        } catch (Exception e) {
            log.warn("Notification skipped (engagement service unavailable): event={}, userId={}", event, userId);
        }
    }

    private void updateOrderToDelivered(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(Order.OrderStatus.DELIVERED);
            orderRepository.save(order);
            log.info("Order {} marked as DELIVERED", order.getOrderNumber());
        });
    }
}
