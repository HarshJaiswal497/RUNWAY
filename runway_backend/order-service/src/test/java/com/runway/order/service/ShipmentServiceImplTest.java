package com.runway.order.service;

import com.runway.order.exception.ResourceNotFoundException;
import com.runway.order.model.Order;
import com.runway.order.model.Shipment;
import com.runway.order.model.Shipment.ShipmentStatus;
import com.runway.order.repository.OrderRepository;
import com.runway.order.repository.ShipmentRepository;
import com.runway.order.util.Dtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentService Unit Tests")
class ShipmentServiceImplTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private OrderRepository    orderRepository;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private Order    sampleOrder;
    private Shipment processingShipment;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
            .id(1L).orderNumber("ORD-TEST0001").userId(1L)
            .status(Order.OrderStatus.PROCESSING)
            .totalAmount(BigDecimal.valueOf(1000))
            .shippingAddress("123 Test St").lines(new ArrayList<>())
            .build();

        processingShipment = Shipment.builder()
            .id(1L).order(sampleOrder)
            .status(ShipmentStatus.PROCESSING)
            .shippingAddress("123 Test St")
            .build();
    }

    // ── trackByOrderId Tests ──────────────────────────────────

    @Test
    @DisplayName("trackByOrderId() — returns shipment for valid order")
    void testTrackByOrderIdSuccess() {
        when(shipmentRepository.findByOrderId(1L))
            .thenReturn(Optional.of(processingShipment));

        ShipmentResponse response = shipmentService.trackByOrderId(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.PROCESSING);
    }

    @Test
    @DisplayName("trackByOrderId() — throws ResourceNotFoundException if not found")
    void testTrackByOrderIdNotFound() {
        when(shipmentRepository.findByOrderId(999L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.trackByOrderId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Shipment not found");
    }

    // ── updateStatus Tests ────────────────────────────────────

    @Test
    @DisplayName("updateStatus() — updates shipment status successfully")
    void testUpdateStatusSuccess() {
        UpdateShipmentStatusRequest request =
            new UpdateShipmentStatusRequest(ShipmentStatus.DISPATCHED);

        when(shipmentRepository.findById(1L))
            .thenReturn(Optional.of(processingShipment));
        when(shipmentRepository.save(any(Shipment.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ShipmentResponse response = shipmentService.updateStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.DISPATCHED);
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    @DisplayName("updateStatus() — sets shippedAt when status is DISPATCHED")
    void testUpdateStatusSetsShippedAt() {
        UpdateShipmentStatusRequest request =
            new UpdateShipmentStatusRequest(ShipmentStatus.DISPATCHED);

        when(shipmentRepository.findById(1L))
            .thenReturn(Optional.of(processingShipment));
        when(shipmentRepository.save(any(Shipment.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        shipmentService.updateStatus(1L, request);

        assertThat(processingShipment.getShippedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus() — sets deliveredAt and updates order when DELIVERED")
    void testUpdateStatusDeliveredSetsTimestamps() {
        UpdateShipmentStatusRequest request =
            new UpdateShipmentStatusRequest(ShipmentStatus.DELIVERED);

        when(shipmentRepository.findById(1L))
            .thenReturn(Optional.of(processingShipment));
        when(shipmentRepository.save(any(Shipment.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(1L))
            .thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class)))
            .thenReturn(sampleOrder);

        shipmentService.updateStatus(1L, request);

        assertThat(processingShipment.getDeliveredAt()).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("updateStatus() — throws ResourceNotFoundException for unknown shipment")
    void testUpdateStatusNotFound() {
        UpdateShipmentStatusRequest request =
            new UpdateShipmentStatusRequest(ShipmentStatus.DISPATCHED);

        when(shipmentRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.updateStatus(999L, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── assignTracking Tests ──────────────────────────────────

    @Test
    @DisplayName("assignTracking() — assigns carrier and tracking number")
    void testAssignTrackingSuccess() {
        AssignTrackingRequest request =
            new AssignTrackingRequest("BlueDart", "BD123456789IN");

        when(shipmentRepository.findById(1L))
            .thenReturn(Optional.of(processingShipment));
        when(shipmentRepository.save(any(Shipment.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ShipmentResponse response = shipmentService.assignTracking(1L, request);

        assertThat(processingShipment.getCarrier()).isEqualTo("BlueDart");
        assertThat(processingShipment.getTrackingNumber()).isEqualTo("BD123456789IN");
    }
}
