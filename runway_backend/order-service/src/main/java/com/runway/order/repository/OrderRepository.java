package com.runway.order.repository;

import com.runway.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** All orders for a specific user, newest first */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Paginated orders for a user */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /** Find order by order number (e.g. ORD-AB12CD34) */
    Optional<Order> findByOrderNumber(String orderNumber);

    /** Orders with a specific status */
    List<Order> findByStatus(Order.OrderStatus status);

    /** All orders with pagination — for Admin view */
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Load order with all lines in one query */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.lines WHERE o.id = :id")
    Optional<Order> findByIdWithLines(Long id);

    /** Count orders by status — for admin dashboard */
    long countByStatus(Order.OrderStatus status);
}
