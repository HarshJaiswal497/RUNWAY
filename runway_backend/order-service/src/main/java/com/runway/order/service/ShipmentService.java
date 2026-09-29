package com.runway.order.service;

import com.runway.order.util.Dtos.*;

/**
 * ShipmentService — Interface
 */
public interface ShipmentService {

    /** Track shipment by order ID */
    ShipmentResponse trackByOrderId(Long orderId);

    /** Update shipment status — Manager/Admin only */
    ShipmentResponse updateStatus(Long shipmentId,
                                  UpdateShipmentStatusRequest request);

    /** Assign tracking number and carrier — Manager/Admin only */
    ShipmentResponse assignTracking(Long shipmentId,
                                    AssignTrackingRequest request);
}
