package com.runway.order.util;

import com.runway.order.model.Cart;
import com.runway.order.model.CartItem;
import com.runway.order.model.Order;
import com.runway.order.model.OrderLine;
import com.runway.order.model.Shipment;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.runway.order.exception.ValidationException;

/**
 * DTOs — Data Transfer Objects
 *
 * WHY DTOs:
 *   - Never expose JPA entities directly to API (circular refs, lazy loading issues)
 *   - Control exactly what fields clients send/receive
 *   - Validation annotations on request DTOs
 *   - Map entities → DTOs in service layer
 */
public class Dtos {

    // ══════════════════════════════════════════════════════════
    // CART DTOs
    // ══════════════════════════════════════════════════════════

    /** Request — add item to cart */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddCartItemRequest {

        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotBlank(message = "Product title is required")
        private String productTitle;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        private BigDecimal unitPrice;

        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100, message = "Quantity cannot exceed 100")
        private int quantity;
    }

    /** Request — update item quantity */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCartItemRequest {
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100, message = "Quantity cannot exceed 100")
        private int quantity;
    }

    /** Response — cart item */
    @Data
    @Builder
    public static class CartItemResponse {
        private Long       id;
        private Long       productId;
        private String     productTitle;
        private int        quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;

        public static CartItemResponse from(CartItem item) {
            return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productTitle(item.getProductTitle())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
        }
    }

    /** Response — full cart */
    @Data
    @Builder
    public static class CartResponse {
        private Long                  id;
        private Long                  userId;
        private List<CartItemResponse> items;
        private BigDecimal            total;
        private int                   totalItems;
        private LocalDateTime         updatedAt;

        public static CartResponse from(Cart cart) {
            List<CartItemResponse> itemResponses = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream()
                    .map(CartItemResponse::from)
                    .collect(Collectors.toList());

            return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .total(cart.getTotal())
                .totalItems(cart.getTotalItemCount())
                .updatedAt(cart.getUpdatedAt())
                .build();
        }
    }

    // ══════════════════════════════════════════════════════════
    // ORDER DTOs
    // ══════════════════════════════════════════════════════════

    /** Request — checkout / place order */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutRequest {
        @NotBlank(message = "Shipping address is required")
        @Size(min = 10, max = 500, message = "Address must be 10-500 characters")
        private String shippingAddress;

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateOrderStatusRequest {
        @NotBlank(message = "Status is required")
        private String status; // String instead of Enum — Payment Service sends plain String

        public Order.OrderStatus getStatusAsEnum() {
            try {
                return Order.OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid order status: " + status);
            }
        }
    }

    /** Response — order line */
    @Data
    @Builder
    public static class OrderLineResponse {
        private Long       id;
        private Long       productId;
        private String     productTitle;
        private int        quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public static OrderLineResponse from(OrderLine line) {
            return OrderLineResponse.builder()
                .id(line.getId())
                .productId(line.getProductId())
                .productTitle(line.getProductTitle())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .lineTotal(line.getLineTotal())
                .build();
        }
    }

    /** Response — full order */
    @Data
    @Builder
    public static class OrderResponse {
        private Long                   id;
        private String                 orderNumber;
        private Long                   userId;
        private Order.OrderStatus      status;
        private BigDecimal             totalAmount;
        private String                 currency;
        private String                 shippingAddress;
        private String                 notes;
        private List<OrderLineResponse> lines;
        private LocalDateTime          createdAt;
        private LocalDateTime          updatedAt;

        public static OrderResponse from(Order order) {
            List<OrderLineResponse> lineResponses = order.getLines() == null
                ? List.of()
                : order.getLines().stream()
                    .map(OrderLineResponse::from)
                    .collect(Collectors.toList());

            return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .lines(lineResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
        }
    }

    // ══════════════════════════════════════════════════════════
    // SHIPMENT DTOs
    // ══════════════════════════════════════════════════════════

    /** Request — update shipment status (Manager/Admin) */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateShipmentStatusRequest {
        @NotNull(message = "Status is required")
        private Shipment.ShipmentStatus status;
    }

    /** Request — assign tracking details (Manager/Admin) */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignTrackingRequest {
        @NotBlank(message = "Carrier is required")
        private String carrier;

        @NotBlank(message = "Tracking number is required")
        private String trackingNumber;
    }

    /** Response — shipment details */
    @Data
    @Builder
    public static class ShipmentResponse {
        private Long                    id;
        private Long                    orderId;
        private String                  carrier;
        private String                  trackingNumber;
        private Shipment.ShipmentStatus status;
        private String                  shippingAddress;
        private LocalDateTime           shippedAt;
        private LocalDateTime           deliveredAt;
        private LocalDateTime           createdAt;
        private LocalDateTime           updatedAt;

        public static ShipmentResponse from(Shipment shipment) {
            return ShipmentResponse.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrder() != null
                    ? shipment.getOrder().getId() : null)
                .carrier(shipment.getCarrier())
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus())
                .shippingAddress(shipment.getShippingAddress())
                .shippedAt(shipment.getShippedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
        }
    }
}
