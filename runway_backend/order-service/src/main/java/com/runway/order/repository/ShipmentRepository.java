package com.runway.order.repository;

import com.runway.order.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    /** Find shipment for a specific order */
    Optional<Shipment> findByOrderId(Long orderId);

    /** Find shipment by tracking number */
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    /** All shipments with a specific status */
    List<Shipment> findByStatus(Shipment.ShipmentStatus status);

    /** Check if shipment exists for order */
    boolean existsByOrderId(Long orderId);
}
