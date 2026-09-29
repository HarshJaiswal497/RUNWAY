//package com.runway.order.controller;
//
//import com.runway.order.config.SecurityConfig;
//import com.runway.order.model.Order.OrderStatus;
//import com.runway.order.service.OrderService;
//import com.runway.order.util.Dtos.CheckoutRequest;
//import com.runway.order.util.Dtos.OrderResponse;
//import com.runway.order.util.Dtos.UpdateOrderStatusRequest;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * OrderControllerTest — Web Layer Tests
// *
// * Controller routes include {userId} in path and require X-User-Id / X-User-Role headers.
// * Admin endpoints check X-User-Role header (not Spring Security role).
// * SecurityConfig uses RoleHeaderFilter + permitAll().
// */
//@WebMvcTest(OrderController.class)
//@Import(SecurityConfig.class)
//@DisplayName("OrderController Tests")
//class OrderControllerTest {
//
//    @Autowired private MockMvc      mockMvc;
//    @Autowired private ObjectMapper objectMapper;
//
//    @MockBean private OrderService     orderService;
//
//    private static final Long USER_ID = 1L;
//
//    private OrderResponse buildSampleOrder() {
//        return OrderResponse.builder()
//                .id(1L)
//                .orderNumber("ORD-TEST0001")
//                .userId(USER_ID)
//                .status(OrderStatus.PENDING_PAYMENT)
//                .totalAmount(BigDecimal.valueOf(1999))
//                .currency("INR")
//                .shippingAddress("123 Test Street")
//                .lines(List.of())
//                .createdAt(LocalDateTime.now())
//                .build();
//    }
//
//    // ── POST /api/orders/{userId} ──────────────────────────────
//
//    @Test
//    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
//    @DisplayName("POST /api/orders/{userId} — returns 201 Created on successful checkout")
//    void placeOrder_returns201_onSuccess() throws Exception {
//        CheckoutRequest req = new CheckoutRequest(
//                "123 Main Street, Chennai - 600001", null);
//
//        when(orderService.placeOrder(eq(USER_ID), any()))
//                .thenReturn(buildSampleOrder());
//
//        mockMvc.perform(post("/api/orders/{userId}", USER_ID)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req))
//                        .header("X-User-Id", USER_ID.toString())
//                        .header("X-User-Role", "CUSTOMER")
//                        .with(csrf()))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.orderNumber").value("ORD-TEST0001"))
//                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
//    }
//
//    @Test
//    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
//    @DisplayName("POST /api/orders/{userId} — returns 400 when shipping address is blank")
//    void placeOrder_returns400_whenAddressBlank() throws Exception {
//        CheckoutRequest req = new CheckoutRequest("", null);
//
//        mockMvc.perform(post("/api/orders/{userId}", USER_ID)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req))
//                        .header("X-User-Id", USER_ID.toString())
//                        .header("X-User-Role", "CUSTOMER")
//                        .with(csrf()))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @DisplayName("POST /api/orders/{userId} — returns 500 when no X-User-Id header")
//    void placeOrder_returns500_whenNoUserIdHeader() throws Exception {
//        CheckoutRequest req = new CheckoutRequest("123 Street, Some City", null);
//
//        mockMvc.perform(post("/api/orders/{userId}", USER_ID)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req))
//                        .with(csrf()))
//                .andExpect(status().isInternalServerError());
//    }
//
//    // ── GET /api/orders/{userId} ───────────────────────────────
//
//    @Test
//    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
//    @DisplayName("GET /api/orders/{userId} — returns 200 with paginated orders")
//    void getMyOrders_returns200_withPaginatedOrders() throws Exception {
//        List<OrderResponse> content = List.of(buildSampleOrder());
//        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
//
//        when(orderService.getMyOrders(eq(USER_ID), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(content, pageable, content.size()));
//
//        mockMvc.perform(get("/api/orders/{userId}", USER_ID)
//                        .header("X-User-Id", USER_ID.toString())
//                        .header("X-User-Role", "CUSTOMER"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//    }
//
//    @Test
//    @DisplayName("GET /api/orders/{userId} — returns 500 when no X-User-Id header")
//    void getMyOrders_returns500_whenNoUserIdHeader() throws Exception {
//        mockMvc.perform(get("/api/orders/{userId}", USER_ID))
//                .andExpect(status().isInternalServerError());
//    }
//
//    // ── GET /api/orders/{userId}/{id} ──────────────────────────
//
//    @Test
//    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
//    @DisplayName("GET /api/orders/{userId}/{id} — returns 200 with order details")
//    void getOrderById_returns200_withOrderDetails() throws Exception {
//        when(orderService.getOrderById(eq(1L), eq(USER_ID)))
//                .thenReturn(buildSampleOrder());
//
//        mockMvc.perform(get("/api/orders/{userId}/{id}", USER_ID, 1L)
//                        .header("X-User-Id", USER_ID.toString())
//                        .header("X-User-Role", "CUSTOMER"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.id").value(1))
//                .andExpect(jsonPath("$.data.orderNumber").value("ORD-TEST0001"));
//    }
//
//    // ── PUT /api/orders/{userId}/{id}/cancel ───────────────────
//
//    @Test
//    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
//    @DisplayName("PUT /api/orders/{userId}/{id}/cancel — returns 200 on successful cancel")
//    void cancelOrder_returns200_onSuccess() throws Exception {
//        OrderResponse cancelled = OrderResponse.builder()
//                .id(1L).orderNumber("ORD-TEST0001").userId(USER_ID)
//                .status(OrderStatus.CANCELLED)
//                .totalAmount(BigDecimal.valueOf(1999))
//                .lines(List.of()).build();
//
//        when(orderService.cancelOrder(eq(1L), eq(USER_ID))).thenReturn(cancelled);
//
//        mockMvc.perform(put("/api/orders/{userId}/{id}/cancel", USER_ID, 1L)
//                        .header("X-User-Id", USER_ID.toString())
//                        .header("X-User-Role", "CUSTOMER")
//                        .with(csrf()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
//    }
//
//    // ── Admin Endpoints ───────────────────────────────────────
//
//    @Test
//    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
//    @DisplayName("GET /api/orders/admin/all — Admin returns 200 with all orders")
//    void getAllOrders_returns200_forAdmin() throws Exception {
//        List<OrderResponse> content = List.of(buildSampleOrder());
//        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
//
//        when(orderService.getAllOrders(any(Pageable.class)))
//                .thenReturn(new PageImpl<>(content, pageable, content.size()));
//
//        mockMvc.perform(get("/api/orders/admin/all")
//                        .header("X-User-Role", "ADMIN"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//    }
//
//    @Test
//    @WithMockUser(username = "customer@test.com", roles = "CUSTOMER")
//    @DisplayName("GET /api/orders/admin/all — returns 403 for non-admin")
//    void getAllOrders_returns403_forCustomer() throws Exception {
//        mockMvc.perform(get("/api/orders/admin/all")
//                        .header("X-User-Role", "CUSTOMER"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
//    @DisplayName("PUT /api/orders/admin/1/status — Admin can update status")
//    void updateOrderStatus_returns200_forAdmin() throws Exception {
//        UpdateOrderStatusRequest req =
//                new UpdateOrderStatusRequest(OrderStatus.PAID);
//
//        when(orderService.updateOrderStatus(eq(1L), any()))
//                .thenReturn(buildSampleOrder());
//
//        mockMvc.perform(put("/api/orders/admin/1/status")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req))
//                        .header("X-User-Role", "ADMIN")
//                        .with(csrf()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//    }
//
//    @Test
//    @WithMockUser(username = "customer@test.com", roles = "CUSTOMER")
//    @DisplayName("PUT /api/orders/admin/1/status — returns 403 for non-admin")
//    void updateOrderStatus_returns403_forCustomer() throws Exception {
//        UpdateOrderStatusRequest req =
//                new UpdateOrderStatusRequest(OrderStatus.PAID);
//
//        mockMvc.perform(put("/api/orders/admin/1/status")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req))
//                        .header("X-User-Role", "CUSTOMER")
//                        .with(csrf()))
//                .andExpect(status().isForbidden());
//    }
//}
//
