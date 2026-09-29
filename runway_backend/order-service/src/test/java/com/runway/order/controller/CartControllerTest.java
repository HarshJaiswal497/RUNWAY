package com.runway.order.controller;

import com.runway.order.config.SecurityConfig;
import com.runway.order.service.CartService;
import com.runway.order.util.Dtos.AddCartItemRequest;
import com.runway.order.util.Dtos.CartItemResponse;
import com.runway.order.util.Dtos.CartResponse;
import com.runway.order.util.Dtos.UpdateCartItemRequest;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CartControllerTest — Web Layer Tests
 *
 * Current controller routes use /my and obtain the user ID
 * from the X-User-Id header.
 *
 * SecurityConfig uses RoleHeaderFilter + permitAll(),
 * so authentication information comes from headers.
 */
@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
@DisplayName("CartController Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private static final Long USER_ID = 1L;

    // ── Helper builders ───────────────────────────────────────

    private CartResponse buildEmptyCart() {
        return CartResponse.builder()
                .id(1L)
                .userId(USER_ID)
                .items(List.of())
                .total(BigDecimal.ZERO)
                .totalItems(0)
                .build();
    }

    private CartResponse buildCartWithItem() {
        CartItemResponse item = CartItemResponse.builder()
                .id(1L)
                .productId(100L)
                .productTitle("Test Product")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(999))
                .subtotal(BigDecimal.valueOf(1998))
                .build();

        return CartResponse.builder()
                .id(1L)
                .userId(USER_ID)
                .items(List.of(item))
                .total(BigDecimal.valueOf(1998))
                .totalItems(2)
                .build();
    }

    // ── GET /api/cart/my ──────────────────────────────────────

    @Test
    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
    @DisplayName("GET /api/cart/my — returns 200 with cart for authenticated user")
    void getMyCart_returns200_forAuthenticatedUser() throws Exception {

        when(cartService.getMyCart(eq(USER_ID)))
                .thenReturn(buildEmptyCart());

        mockMvc.perform(
                        get("/api/cart/my")
                                .header("X-User-Id", USER_ID.toString())
                                .header("X-User-Role", "CUSTOMER")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.totalItems").value(0));
    }

    @Test
    @DisplayName("GET /api/cart/my — returns 500 when no X-User-Id header")
    void getMyCart_returns500_whenNoUserIdHeader() throws Exception {

        mockMvc.perform(
                        get("/api/cart/my")
                )
                .andExpect(status().isInternalServerError());
    }

    // ── POST /api/cart/my/items ───────────────────────────────

    @Test
    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
    @DisplayName("POST /api/cart/my/items — returns 200 when item added successfully")
    void addItem_returns200_onSuccess() throws Exception {

        AddCartItemRequest req = new AddCartItemRequest(
                100L,
                "Test Product",
                BigDecimal.valueOf(999),
                2
        );

        when(cartService.addItem(eq(USER_ID), any()))
                .thenReturn(buildCartWithItem());

        mockMvc.perform(
                        post("/api/cart/my/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .header("X-User-Id", USER_ID.toString())
                                .header("X-User-Role", "CUSTOMER")
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.total").value(1998));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
    @DisplayName("POST /api/cart/my/items — returns 400 when productId is null")
    void addItem_returns400_whenProductIdNull() throws Exception {

        AddCartItemRequest req = new AddCartItemRequest(
                null,
                "Test Product",
                BigDecimal.valueOf(999),
                2
        );

        mockMvc.perform(
                        post("/api/cart/my/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .header("X-User-Id", USER_ID.toString())
                                .header("X-User-Role", "CUSTOMER")
                                .with(csrf())
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
    @DisplayName("POST /api/cart/my/items — returns 400 when quantity is 0")
    void addItem_returns400_whenQuantityZero() throws Exception {

        AddCartItemRequest req = new AddCartItemRequest(
                100L,
                "Product",
                BigDecimal.valueOf(999),
                0
        );

        mockMvc.perform(
                        post("/api/cart/my/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .header("X-User-Id", USER_ID.toString())
                                .header("X-User-Role", "CUSTOMER")
                                .with(csrf())
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
    @DisplayName("POST /api/cart/my/items — returns 400 when product title is blank")
    void addItem_returns400_whenProductTitleBlank() throws Exception {

        AddCartItemRequest req = new AddCartItemRequest(
                100L,
                "",
                BigDecimal.valueOf(999),
                1
        );

        mockMvc.perform(
                        post("/api/cart/my/items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .header("X-User-Id", USER_ID.toString())
                                .header("X-User-Role", "CUSTOMER")
                                .with(csrf())
                )
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/cart/my/items/{productId} ────────────────────

    @Test
    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
    @DisplayName("PUT /api/cart/my/items/100 — returns 200 on quantity update")
    void updateItemQuantity_returns200_onSuccess() throws Exception {

        UpdateCartItemRequest req = new UpdateCartItemRequest(5);

        when(cartService.updateItemQuantity(eq(USER_ID), eq(100L), any()))
                .thenReturn(buildCartWithItem());

        mockMvc.perform(
                        put("/api/cart/my/items/{productId}", 100L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                                .header("X-User-Id", USER_ID.toString())
                                .header("X-User-Role", "CUSTOMER")
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── DELETE /api/cart/my/items/{productId} ─────────────────

    @Test
    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
    @DisplayName("DELETE /api/cart/my/items/100 — returns 200 on item removal")
    void removeItem_returns200_onSuccess() throws Exception {

        when(cartService.removeItem(eq(USER_ID), eq(100L)))
                .thenReturn(buildEmptyCart());

        mockMvc.perform(
                        delete("/api/cart/my/items/{productId}", 100L)
                                .header("X-User-Id", USER_ID.toString())
                                .header("X-User-Role", "CUSTOMER")
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── DELETE /api/cart/my ───────────────────────────────────

    @Test
    @WithMockUser(username = "test@test.com", roles = "CUSTOMER")
    @DisplayName("DELETE /api/cart/my — returns 200 on cart clear")
    void clearCart_returns200_onSuccess() throws Exception {

        when(cartService.clearCart(eq(USER_ID)))
                .thenReturn(buildEmptyCart());

        mockMvc.perform(
                        delete("/api/cart/my")
                                .header("X-User-Id", USER_ID.toString())
                                .header("X-User-Role", "CUSTOMER")
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}