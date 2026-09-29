import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { ShipmentService } from '../../core/services/shipment.service';
import { ProductImageService } from '../../core/services/product-image.service';
import { OrderResponse, OrderStatus, ShipmentResponse } from '../../core/models/order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.css'
})
export class OrderDetailComponent implements OnInit {
  private auth        = inject(AuthService);
  private orderSvc    = inject(OrderService);
  private shipmentSvc = inject(ShipmentService);
  private imageSvc    = inject(ProductImageService);
  private route       = inject(ActivatedRoute);
  private router      = inject(Router);

  loading         = signal(true);
  cancelling      = signal(false);
  error           = signal<string | null>(null);
  order           = signal<OrderResponse | null>(null);
  shipment        = signal<ShipmentResponse | null>(null);
  shipmentLoading = signal(false);
  imageMap        = signal<Map<number, string>>(new Map());

  canCancel = computed(() => this.order()?.status === 'PENDING_PAYMENT');

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }

    const orderId = Number(this.route.snapshot.paramMap.get('id'));
    if (!orderId) { this.router.navigate(['/orders']); return; }

    // No userId — backend resolves from JWT
    this.orderSvc.getOrderById(orderId).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
        this.loadShipment(orderId);
        this.loadImages(o);
      },
      error: () => { this.error.set('Order not found.'); this.loading.set(false); }
    });
  }

  private loadImages(order: OrderResponse): void {
    const ids = order.lines.map(l => l.productId);
    if (!ids.length) return;
    this.imageSvc.getImageUrls(ids).subscribe(map => this.imageMap.set(map));
  }

  private loadShipment(orderId: number): void {
    this.shipmentLoading.set(true);
    this.shipmentSvc.getByOrderIdSafe(orderId).subscribe(s => {
      this.shipment.set(s);
      this.shipmentLoading.set(false);
    });
  }

  getLineImage(productId: number): string {
    return this.imageMap().get(productId) ?? '/placeholder.jpg';
  }

  cancelOrder(): void {
    const orderId = this.order()?.id;
    if (!orderId) return;

    this.cancelling.set(true);
    // No userId — backend resolves from JWT
    this.orderSvc.cancelOrder(orderId).subscribe({
      next: (updated) => { this.order.set(updated); this.cancelling.set(false); },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Could not cancel the order.');
        this.cancelling.set(false);
      }
    });
  }

  statusClass(status: OrderStatus): string {
    const map: Record<OrderStatus, string> = {
      PENDING_PAYMENT: 'status-pending',  PAID: 'status-paid',
      PROCESSING: 'status-processing',    SHIPPED: 'status-shipped',
      DELIVERED: 'status-delivered',      COMPLETED: 'status-completed',
      CANCELLED: 'status-cancelled',      REFUNDED: 'status-refunded'
    };
    return map[status] ?? '';
  }

  statusLabel(s: string): string { return s.replace(/_/g, ' '); }

  trackShipment(): void {
    const id = this.order()?.id;
    if (id) this.router.navigate(['/orders', id, 'tracking']);
  }
}