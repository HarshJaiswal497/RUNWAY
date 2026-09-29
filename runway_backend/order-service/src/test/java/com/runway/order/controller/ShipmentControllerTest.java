package com.runway.order.controller;

import com.runway.order.config.SecurityConfig;
import com.runway.order.model.Shipment.ShipmentStatus;
import com.runway.order.service.ShipmentService;
import com.runway.order.util.Dtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ShipmentControllerTest — Web Layer Tests
 *
 * ShipmentController has NO role-based authorization (DEV MODE — all endpoints open).
 * SecurityConfig uses RoleHeaderFilter + permitAll().
 * So unauthenticated requests return 200 (not 401), and any role can call any endpoint.
 */
@WebMvcTest(ShipmentController.class)
@Import(SecurityConfig.class)
@DisplayName("ShipmentController Tests")
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentService shipmentService;

    private ShipmentResponse buildShipment(ShipmentStatus status) {
        return ShipmentResponse.builder()
                .id(1L)
                .orderId(10L)
                .status(status)
                .shippingAddress("123 Test Street")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── GET /api/shipments/order/{orderId} ────────────────────

    @Test
    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
    @DisplayName("GET /api/shipments/order/10 — returns 200 with shipment")
    void testTrackShipmentReturns200() throws Exception {
        when(shipmentService.trackByOrderId(10L))
                .thenReturn(buildShipment(ShipmentStatus.IN_TRANSIT));

        mockMvc.perform(get("/api/shipments/order/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.data.orderId").value(10));
    }

    @Test
    @DisplayName("GET /api/shipments/order/10 — returns 200 even without auth (permitAll)")
    void testTrackShipmentUnauthenticatedReturns200() throws Exception {
        when(shipmentService.trackByOrderId(10L))
                .thenReturn(buildShipment(ShipmentStatus.IN_TRANSIT));

        mockMvc.perform(get("/api/shipments/order/10"))
                .andExpect(status().isOk());
    }

    // ── PUT /api/shipments/{id}/status ────────────────────────

    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    @DisplayName("PUT /api/shipments/1/status — Manager can update status")
    void testUpdateStatusManagerReturns200() throws Exception {
        UpdateShipmentStatusRequest req =
                new UpdateShipmentStatusRequest(ShipmentStatus.DISPATCHED);

        when(shipmentService.updateStatus(eq(1L), any()))
                .thenReturn(buildShipment(ShipmentStatus.DISPATCHED));

        mockMvc.perform(put("/api/shipments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"));
    }

    @Test
    @WithMockUser(username = "customer@test.com", roles = "CUSTOMER")
    @DisplayName("PUT /api/shipments/1/status — Customer can also update (no role check in DEV mode)")
    void testUpdateStatusCustomerAlsoAllowed() throws Exception {
        UpdateShipmentStatusRequest req = new UpdateShipmentStatusRequest(ShipmentStatus.DISPATCHED);

        when(shipmentService.updateStatus(eq(1L), any()))
                .thenReturn(buildShipment(ShipmentStatus.DISPATCHED));

        mockMvc.perform(put("/api/shipments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("PUT /api/shipments/1/status — Admin can update status")
    void testUpdateStatusAdminReturns200() throws Exception {
        UpdateShipmentStatusRequest req =
                new UpdateShipmentStatusRequest(ShipmentStatus.DELIVERED);

        when(shipmentService.updateStatus(eq(1L), any()))
                .thenReturn(buildShipment(ShipmentStatus.DELIVERED));

        mockMvc.perform(put("/api/shipments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));
    }

    // ── PUT /api/shipments/{id}/tracking ──────────────────────

    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    @DisplayName("PUT /api/shipments/1/tracking — assigns tracking details")
    void testAssignTrackingReturns200() throws Exception {
        AssignTrackingRequest req =
                new AssignTrackingRequest("BlueDart", "BD123456789IN");

        ShipmentResponse response = buildShipment(ShipmentStatus.DISPATCHED);
        when(shipmentService.assignTracking(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/shipments/1/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    @DisplayName("PUT /api/shipments/1/tracking — returns 400 for blank carrier")
    void testAssignTrackingBlankCarrierReturns400() throws Exception {
        AssignTrackingRequest req = new AssignTrackingRequest("", "BD123");

        mockMvc.perform(put("/api/shipments/1/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}

