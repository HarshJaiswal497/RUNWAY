import { Injectable, inject } from '@angular/core';
import { Observable, map, catchError, of } from 'rxjs';
import { ApiService } from './api.service';
import { ApiResponse, ShipmentResponse } from '../models/order.model';

// ─────────────────────────────────────────────────────────────────────────────
// ShipmentService
//
// Wraps the single customer-facing shipment endpoint.
// Base path: /api/shipments
//
// IMPORTANT — shipment rows do NOT exist for every order.
// The backend auto-creates a shipment ONLY when an Admin calls:
//   PUT /api/orders/admin/{id}/status  with body { "status": "PROCESSING" }
//
// This means:
//   - Orders in PENDING_PAYMENT or PAID status → no shipment row → 404
//   - Orders in PROCESSING or later            → shipment row exists → 200
//
// Both places that call getByOrderId() handle 404 silently:
//   - OrderDetailComponent: shows "shipment not yet created" section
//   - TrackingComponent:    shows "not yet available" message
//
// ShipmentResponse key fields:
//   id, orderId, status (ShipmentStatus), carrier (nullable),
//   trackingNumber (nullable), shippingAddress,
//   shippedAt (nullable — set when status → DISPATCHED),
//   deliveredAt (nullable — set when status → DELIVERED),
//   createdAt, updatedAt
//
// ShipmentStatus enum (6 values from backend Shipment.ShipmentStatus):
//   PROCESSING       → shipment created, preparing to ship
//   DISPATCHED       → handed to carrier (shippedAt is set here)
//   IN_TRANSIT       → on the way
//   OUT_FOR_DELIVERY → last-mile delivery
//   DELIVERED        → delivered (deliveredAt is set here)
//   RETURNED         → returned to warehouse (terminal failure state)
//
// Progress stepper shows 5 steps: PROCESSING → DISPATCHED → IN_TRANSIT
//   → OUT_FOR_DELIVERY → DELIVERED
// RETURNED is shown as a separate failure banner, not on the stepper.
// ─────────────────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class ShipmentService {
  private api = inject(ApiService);

  // ─────────────────────────────────────────────────────────────────────────
  // GET /api/shipments/order/{orderId}
  //
  // Fetches shipment details for a given order ID.
  // No auth header check on this endpoint in the backend — it only requires
  // the JWT (handled by authInterceptor). The orderId in the path is enough.
  //
  // *** 404 is EXPECTED and NORMAL for new orders ***
  // The backend throws ResourceNotFoundException("Shipment not found for
  // order: {orderId}") when no shipment exists yet.
  // DO NOT show this as an error to the user — handle it silently.
  //
  // Usage pattern 1 — OrderDetailComponent (inline shipment preview):
  //   Call after order loads. On 404 → shipment signal stays null →
  //   "Shipment will be created once order enters processing" section shows.
  //   On success → show carrier, trackingNumber, status inline + Track button.
  //
  // Usage pattern 2 — TrackingComponent (full tracking page):
  //   On 404 → show "Tracking information not yet available" state message.
  //   On success → show metadata block + progress stepper.
  //
  // Both components handle the error in their own subscribe error callback.
  // This service exposes two versions:
  //   getByOrderId()         — raw Observable, caller handles error
  //   getByOrderIdSafe()     — returns null on 404, never errors (convenience)
  //
  // Called by:
  //   - OrderDetailComponent.loadShipment(orderId)
  //   - TrackingComponent.ngOnInit()
  // ─────────────────────────────────────────────────────────────────────────
  getByOrderId(orderId: number): Observable<ShipmentResponse> {
    return this.api
      .get<ApiResponse<ShipmentResponse>>(`/api/shipments/order/${orderId}`)
      .pipe(map(r => r.data));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // GET /api/shipments/order/{orderId}  — null-safe version
  //
  // Same endpoint as getByOrderId() but catches ALL errors and returns
  // null instead of propagating them.
  //
  // Use this in OrderDetailComponent where a missing shipment should
  // simply hide the shipment section — no error state needed.
  //
  // Usage:
  //   this.shipmentSvc.getByOrderIdSafe(orderId).subscribe(shipment => {
  //     this.shipment.set(shipment);   // null if 404 or any error
  //   });
  //
  // Do NOT use this in TrackingComponent — the tracking page needs to
  // distinguish between "not ready yet" (show message) vs a real error,
  // so use getByOrderId() there and handle errors explicitly.
  // ─────────────────────────────────────────────────────────────────────────
  getByOrderIdSafe(orderId: number): Observable<ShipmentResponse | null> {
    return this.api
      .get<ApiResponse<ShipmentResponse>>(`/api/shipments/order/${orderId}`)
      .pipe(
        map(r => r.data),
        catchError(() => of(null))    // 404 or any error → return null
      );
  }
}