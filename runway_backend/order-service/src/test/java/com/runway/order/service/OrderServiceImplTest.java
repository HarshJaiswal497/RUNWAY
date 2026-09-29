package com.runway.order.service;

import com.runway.order.client.CatalogServiceClient;
import com.runway.order.exception.ResourceNotFoundException;
import com.runway.order.exception.ValidationException;
import com.runway.order.model.Cart;
import com.runway.order.model.CartItem;
import com.runway.order.model.Order;
import com.runway.order.model.Order.OrderStatus;
import com.runway.order.repository.CartItemRepository;
import com.runway.order.repository.CartRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceImplTest {

    @Mock private OrderRepository       orderRepository;
    @Mock private CartRepository        cartRepository;
    @Mock private CartItemRepository    cartItemRepository;
    @Mock private ShipmentRepository    shipmentRepository;
    @Mock private CatalogServiceClient  catalogClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final Long USER_ID = 1L;

    private Cart emptyCart;
    private Cart cartWithItems;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        emptyCart = Cart.builder()
            .id(1L).userId(USER_ID).items(new ArrayList<>()).build();

        CartItem item = CartItem.builder()
            .id(1L).cart(emptyCart).productId(100L)
            .productTitle("Test Product").quantity(2)
            .unitPrice(BigDecimal.valueOf(500)).build();

        cartWithItems = Cart.builder()
            .id(1L).userId(USER_ID)
            .items(new ArrayList<>(List.of(item))).build();

        pendingOrder = Order.builder()
            .id(10L).orderNumber("ORD-TEST0001").userId(USER_ID)
            .status(OrderStatus.PENDING_PAYMENT)
            .totalAmount(BigDecimal.valueOf(1000))
            .shippingAddress("123 Test Street").lines(new ArrayList<>())
            .build();
    }

    // ── placeOrder Tests ──────────────────────────────────────

    @Test
    @DisplayName("placeOrder() — creates order from cart successfully")
    void testPlaceOrderSuccess() {
        CheckoutRequest request = new CheckoutRequest(
            "123 Main Street, Chennai - 600001", null);

        // Mock Feign: stock check + stock deduction
        when(catalogClient.getStock(100L)).thenReturn(100);
        doNothing().when(catalogClient).deductStock(eq(100L), anyInt(), anyLong(), anyString());

        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(cartWithItems));
        when(orderRepository.save(any(Order.class)))
            .thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                return Order.builder()
                    .id(1L).orderNumber("ORD-ABCD1234")
                    .userId(o.getUserId()).status(o.getStatus())
                    .totalAmount(o.getTotalAmount())
                    .shippingAddress(o.getShippingAddress())
                    .lines(new ArrayList<>()).build();
            });

        OrderResponse response = orderService.placeOrder(USER_ID, request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.getTotalAmount())
            .isEqualByComparingTo(BigDecimal.valueOf(1000));

        verify(orderRepository).save(any(Order.class));
        verify(cartItemRepository).deleteAllByCartId(1L);
    }

    @Test
    @DisplayName("placeOrder() — throws ValidationException when cart is empty")
    void testPlaceOrderEmptyCartThrows() {
        CheckoutRequest request = new CheckoutRequest("123 Street, Some City", null);

        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("cart is empty");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("placeOrder() — throws ValidationException when cart not found")
    void testPlaceOrderCartNotFoundThrows() {
        CheckoutRequest request = new CheckoutRequest("123 Street, Some City", null);

        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, request))
            .isInstanceOf(ValidationException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("placeOrder() — throws ValidationException for blank address")
    void testPlaceOrderBlankAddressThrows() {
        CheckoutRequest request = new CheckoutRequest("", null);

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, request))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Shipping address");
    }

    @Test
    @DisplayName("placeOrder() — cart is cleared after successful checkout")
    void testPlaceOrderClearsCart() {
        CheckoutRequest request = new CheckoutRequest("123 Street, City Area", null);

        // Mock Feign: stock check + stock deduction
        when(catalogClient.getStock(100L)).thenReturn(100);
        doNothing().when(catalogClient).deductStock(eq(100L), anyInt(), anyLong(), anyString());

        when(cartRepository.findByUserIdWithItems(USER_ID))
            .thenReturn(Optional.of(cartWithItems));
        when(orderRepository.save(any(Order.class)))
            .thenReturn(pendingOrder);

        orderService.placeOrder(USER_ID, request);

        verify(cartItemRepository).deleteAllByCartId(1L);
    }

    // ── cancelOrder Tests ─────────────────────────────────────

    @Test
    @DisplayName("cancelOrder() — cancels PENDING_PAYMENT order")
    void testCancelOrderSuccess() {
        when(orderRepository.findById(10L))
            .thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findByIdWithLines(10L))
            .thenReturn(Optional.of(pendingOrder));

        OrderResponse response = orderService.cancelOrder(10L, USER_ID);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(pendingOrder);
    }

    @Test
    @DisplayName("cancelOrder() — throws ValidationException for PAID order")
    void testCancelPaidOrderThrows() {
        Order paidOrder = Order.builder()
            .id(10L).userId(USER_ID).status(OrderStatus.PAID)
            .totalAmount(BigDecimal.valueOf(1000))
            .shippingAddress("addr").lines(new ArrayList<>()).build();

        when(orderRepository.findById(10L))
            .thenReturn(Optional.of(paidOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(10L, USER_ID))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("PENDING_PAYMENT");
    }

    @Test
    @DisplayName("cancelOrder() — throws ValidationException for another user's order")
    void testCancelOtherUserOrderThrows() {
        Long otherUserId = 99L;
        Order otherOrder = Order.builder()
            .id(10L).userId(otherUserId).status(OrderStatus.PENDING_PAYMENT)
            .totalAmount(BigDecimal.valueOf(1000))
            .shippingAddress("addr").lines(new ArrayList<>()).build();

        when(orderRepository.findById(10L))
            .thenReturn(Optional.of(otherOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(10L, USER_ID))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("permission");
    }

    @Test
    @DisplayName("cancelOrder() — throws ResourceNotFoundException for unknown order")
    void testCancelNonExistentOrderThrows() {
        when(orderRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(999L, USER_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getOrderById Tests ────────────────────────────────────

    @Test
    @DisplayName("getOrderById() — returns order for owner")
    void testGetOrderByIdForOwner() {
        when(orderRepository.findByIdWithLines(10L))
            .thenReturn(Optional.of(pendingOrder));

        OrderResponse response = orderService.getOrderById(10L, USER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getOrderById() — throws ResourceNotFoundException for unknown order")
    void testGetOrderByIdNotFound() {
        when(orderRepository.findByIdWithLines(999L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L, USER_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
