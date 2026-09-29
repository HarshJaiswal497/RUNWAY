package com.runway.order.repository;

import com.runway.order.model.Order;
import com.runway.order.model.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    /** All lines for a specific order */
    List<OrderLine> findByOrderId(Long orderId);

    /**
     * Returns true when the user has a line-item for the given product
     * in an order whose status is any of the provided statuses.
     * Using IN allows callers to treat DELIVERED and COMPLETED equivalently.
     */
    @Query("""
        select case when count(ol) > 0 then true else false end
        from OrderLine ol
        join ol.order o
        where o.userId    = :userId
          and ol.productId = :productId
          and o.status    in :statuses
        """)
    boolean existsDeliveredProductForUser(
            @Param("userId")   Long userId,
            @Param("productId") Long productId,
            @Param("statuses") Collection<Order.OrderStatus> statuses
    );
}
