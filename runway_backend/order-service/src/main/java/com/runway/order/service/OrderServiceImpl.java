package com.runway.order.service;

import com.runway.order.client.CatalogServiceClient;
import com.runway.order.client.EngagementServiceClient;
import com.runway.order.exception.ResourceNotFoundException;
import com.runway.order.exception.ValidationException;
import com.runway.order.model.Cart;
import com.runway.order.model.Order;
import com.runway.order.model.Order.OrderStatus;
import com.runway.order.model.OrderLine;
import com.runway.order.model.Shipment;
import com.runway.order.repository.*;
import com.runway.order.util.Dtos.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OrderServiceImpl — Business Logic for Orders
 *
 * CHECKOUT FLOW (IMPROVED):
 *   1. Validate shipping address
 *   2. Load cart and validate not empty
 *   3. Feign: Verify stock for ALL cart items via Catalog Service
 *   4. Feign: Deduct stock FIRST (before saving order)
 *   5. Convert cart items → order lines (price snapshot)
 *   6. Build and save order with status PENDING_PAYMENT
 *   7. Clear the cart
 *   8. Return order response
 *
 *   If stock deduction fails: Compensate already-deducted items and throw exception
 *   If DB save fails: Compensate all deducted items and throw exception
 *
 * CANCEL FLOW:
 *   1. Validate order exists and is PENDING_PAYMENT
 *   2. Set status to CANCELLED
 *   3. Feign: Restore stock back to Catalog Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ShipmentRepository shipmentRepository;
	private final CatalogServiceClient catalogClient;
	private final EngagementServiceClient engagementClient;

	@Override
	@Transactional
	public OrderResponse placeOrder(Long userId, CheckoutRequest request) {
		// 1. Validate shipping address
		if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
			throw new ValidationException("Shipping address is required.");
		}

		// 2. Load cart and validate not empty
		Cart cart = cartRepository
			.findByUserIdWithItems(userId)
			.orElseThrow(() -> new ValidationException("Your cart is empty. Add items first."));

		if (cart.isEmpty()) {
			throw new ValidationException("Your cart is empty. Add items before checkout.");
		}

		// 3. Verify stock for ALL cart items BEFORE attempting deduction
		for (var item : cart.getItems()) {
			try {
				Integer stock = catalogClient.getStock(item.getProductId());
				log.debug("Available Stock: {}", stock);
				if (stock < item.getQuantity()) {
					throw new ValidationException(
						"Insufficient stock for '" +
						item.getProductTitle() +
						"'. Available: " +
						stock +
						", In cart: " +
						item.getQuantity()
					);
				}
			} catch (ValidationException ve) {
				throw ve;
			} catch (Exception e) {
				log.error("Stock check failed for product {}: {}", item.getProductId(), e.getMessage());
				throw new ValidationException(
					"Unable to verify stock for '" + item.getProductTitle() + "'. Please try again later."
				);
			}
		}

		// 4. DEDUCT STOCK FIRST (track successes for compensation)
		List<OrderLine> deductedLines = new ArrayList<>();
		log.debug("Deducted Lines: {}", deductedLines);
		try {
			log.debug("Here, Deducted Lines -->");
			for (var item : cart.getItems()) {
				log.debug("Inside for loop -->");
				try {
					log.debug("Here: trying to deduct stock--> [1]");
					catalogClient.deductStock(
						item.getProductId(),
						-item.getQuantity(), // negative = deduct
						userId,
						"ADMIN" // system-level deduction
					);
					log.debug("Here: trying to deduct stock--> [2]");

					// Track this line for later compensation if needed
					deductedLines.add(
						OrderLine
							.builder()
							.productId(item.getProductId())
							.productTitle(item.getProductTitle())
							.quantity(item.getQuantity())
							.unitPrice(item.getUnitPrice())
							.lineTotal(item.getSubtotal())
							.build()
					);

					log.info("Stock deducted: productId={}, qty=-{}", item.getProductId(), item.getQuantity());
				} catch (Exception e) {
					log.error(
						"Failed to deduct stock for productId={}, qty={}: {}",
						item.getProductId(),
						item.getQuantity(),
						e.getMessage()
					);

					// Compensate already-deducted items
					compensateDeductedStock(deductedLines, userId);

					throw new ValidationException(
						"Unable to reserve stock for productId=" + item.getProductId() + ". Transaction cancelled."
					);
				}
			}

			// 5. All stock deductions succeeded - now safe to build order
			var totalAmount = cart.getTotal();

			Order order = Order
				.builder()
				.userId(userId)
				.status(OrderStatus.PENDING_PAYMENT)
				.totalAmount(totalAmount)
				.shippingAddress(request.getShippingAddress().trim())
				.notes(request.getNotes())
				.build();

			deductedLines.forEach(line -> line.setOrder(order));
			order.setLines(deductedLines);

			// 6. Save order
			Order savedOrder = orderRepository.save(order);
			log.info("Order placed: {} | userId={} | total={}", savedOrder.getOrderNumber(), userId, totalAmount);

			// 7. Clear the cart after successful save
			cartItemRepository.deleteAllByCartId(cart.getId());
			cart.getItems().clear();

			// 8. Notify
			notifyEngagement("ORDER_PLACED", userId, Map.of(
				"orderNumber", savedOrder.getOrderNumber(),
				"amount", totalAmount.toString(),
				"shippingAddress", savedOrder.getShippingAddress()
			));

			return OrderResponse.from(savedOrder);
		} catch (Exception dbEx) {
			log.error("Database error during order creation: {}", dbEx.getMessage());

			// Compensate all deducted stock if DB save fails
			compensateDeductedStock(deductedLines, userId);

			throw dbEx;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public OrderResponse getOrderById(Long orderId, Long userId) {
		Order order = orderRepository
			.findByIdWithLines(orderId)
			.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

		// Security: customer can only view their own order
		if (!order.getUserId().equals(userId) && !isAdmin()) {
			throw new ValidationException("You do not have permission to view this order.");
		}

		return OrderResponse.from(order);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
		return orderRepository.findByUserId(userId, pageable).map(OrderResponse::from);
	}
    @Override
    @Transactional(readOnly = true)
    public boolean hasUserPurchasedProduct(Long userId, Long productId, String status) {
        Order.OrderStatus orderStatus;
        try {
            orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid order status: " + status);
        }
        // DELIVERED and COMPLETED both mean the customer has physically received
        // the item, so treat them as equivalent qualifying statuses.
        java.util.Set<Order.OrderStatus> qualifying =
                orderStatus == Order.OrderStatus.DELIVERED
                        ? java.util.Set.of(Order.OrderStatus.DELIVERED, Order.OrderStatus.COMPLETED)
                        : java.util.Set.of(orderStatus);
        return orderLineRepository.existsDeliveredProductForUser(userId, productId, qualifying);
    }

	@Override
	@Transactional(readOnly = true)
	public Page<OrderResponse> getAllOrders(Pageable pageable) {
		return orderRepository.findAllByOrderByCreatedAtDesc(pageable).map(OrderResponse::from);
	}

	@Override
	@Transactional
	public OrderResponse cancelOrder(Long orderId, Long userId) {
		Order order = orderRepository
			.findById(orderId)
			.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

		// Security: only owner or admin can cancel
		if (!order.getUserId().equals(userId) && !isAdmin()) {
			throw new ValidationException("You do not have permission to cancel this order.");
		}

		// Business rule: only PENDING_PAYMENT orders can be cancelled
		if (!order.canBeCancelled()) {
			throw new ValidationException(
				"Only PENDING_PAYMENT orders can be cancelled. Current status: " + order.getStatus()
			);
		}

		order.setStatus(OrderStatus.CANCELLED);
		Order saved = orderRepository.save(order);
		log.info("Order cancelled: {} | userId={}", order.getOrderNumber(), userId);

		notifyEngagement("ORDER_CANCELLED", order.getUserId(), Map.of(
			"orderNumber", order.getOrderNumber()
		));

		// Restore stock for each order line
		Order orderWithLines = orderRepository.findByIdWithLines(orderId).orElse(saved);
		for (var line : orderWithLines.getLines()) {
			try {
				catalogClient.deductStock(
					line.getProductId(),
					line.getQuantity(), // positive = restore stock
					userId,
					"ADMIN"
				);
				log.info("Stock restored: productId={}, qty=+{}", line.getProductId(), line.getQuantity());
			} catch (Exception e) {
				log.error("Failed to restore stock for productId={}: {}", line.getProductId(), e.getMessage());
			}
		}

		return OrderResponse.from(saved);
	}

	@Override
	@Transactional
	public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
		Order order = orderRepository
			.findById(orderId)
			.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

		OrderStatus oldStatus = order.getStatus();
        order.setStatus(request.getStatusAsEnum());

        if (request.getStatusAsEnum() == Order.OrderStatus.PROCESSING && !shipmentRepository.existsByOrderId(orderId)) {
			Shipment shipment = Shipment
				.builder()
				.order(order)
				.status(Shipment.ShipmentStatus.PROCESSING)
				.shippingAddress(order.getShippingAddress())
				.build();
			shipmentRepository.save(shipment);
			log.info("Auto-created shipment for order: {}", order.getOrderNumber());
		}

		Order saved = orderRepository.save(order);
		log.info("Order status updated: {} | {} → {}", order.getOrderNumber(), oldStatus, request.getStatus());

		return OrderResponse.from(saved);
	}

	private void notifyEngagement(String event, Long userId, Map<String, Object> payload) {
		try {
			engagementClient.notify(new EngagementServiceClient.NotifyRequest(event, userId, payload));
		} catch (Exception e) {
			log.warn("Notification skipped (engagement service unavailable): event={}, userId={}", event, userId);
		}
	}

	/**
	 * Helper: Compensate (restore) already-deducted stock.
	 * Called when a later step fails to maintain consistency.
	 */
	private void compensateDeductedStock(List<OrderLine> deductedLines, Long userId) {
		for (var line : deductedLines) {
			try {
				catalogClient.deductStock(
					line.getProductId(),
					line.getQuantity(), // positive = restore
					userId,
					"ADMIN"
				);
				log.warn("Compensated stock: productId={}, qty=+{}", line.getProductId(), line.getQuantity());
			} catch (Exception compensateEx) {
				log.error(
					"CRITICAL: Compensation failed for productId={}: {}",
					line.getProductId(),
					compensateEx.getMessage()
				);
				// This is a serious issue - log it but don't throw to avoid masking the original error
			}
		}
	}

	private boolean isAdmin() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) return false;
		return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
	}
}
