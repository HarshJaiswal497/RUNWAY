import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { ProductImageService } from '../../core/services/product-image.service';
import { OrderResponse, OrderStatus } from '../../core/models/order.model';

export interface OrderWithImage {
  order: OrderResponse;
  imageMap: Map<number, string>;
}

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-orders.component.html',
  styleUrl: './my-orders.component.css'
})
export class MyOrdersComponent implements OnInit {
  private auth     = inject(AuthService);
  private orderSvc = inject(OrderService);
  private imageSvc = inject(ProductImageService);
  private router   = inject(Router);

  loading     = signal(true);
  error       = signal<string | null>(null);
  orders      = signal<OrderWithImage[]>([]);
  currentPage = signal(0);
  totalPages  = signal(1);
  totalItems  = signal(0);
  readonly pageSize = 8;

  isEmpty = computed(() => !this.loading() && this.orders().length === 0);

  ngOnInit(): void { this.loadOrders(0); }

  loadOrders(page: number): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }

    this.loading.set(true);
    this.error.set(null);

    // No userId — backend resolves from JWT
    this.orderSvc.getMyOrders(page, this.pageSize).subscribe({
      next: (res) => {
        this.currentPage.set(res.number);
        this.totalPages.set(res.totalPages);
        this.totalItems.set(res.totalElements);

        if (res.content.length === 0) {
          this.orders.set([]);
          this.loading.set(false);
          return;
        }

        const allProductIds = [
          ...new Set(res.content.flatMap(o => o.lines.map(l => l.productId)))
        ];

        this.imageSvc.getImageUrls(allProductIds).subscribe(imageMap => {
          this.orders.set(res.content.map(order => ({ order, imageMap })));
          this.loading.set(false);
        });
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Failed to load orders.');
        this.loading.set(false);
      }
    });
  }

  getFirstImage(row: OrderWithImage): string {
    return row.imageMap.get(row.order.lines[0]?.productId) ?? '/placeholder.jpg';
  }

  getLineImage(row: OrderWithImage, productId: number): string {
    return row.imageMap.get(productId) ?? '/placeholder.jpg';
  }

  goToDetail(orderId: number): void { this.router.navigate(['/orders', orderId]); }

  prevPage(): void { if (this.currentPage() > 0) this.loadOrders(this.currentPage() - 1); }
  nextPage(): void { if (this.currentPage() < this.totalPages() - 1) this.loadOrders(this.currentPage() + 1); }

  statusClass(status: OrderStatus): string {
    const map: Record<OrderStatus, string> = {
      PENDING_PAYMENT: 'status-pending',  PAID: 'status-paid',
      PROCESSING: 'status-processing',    SHIPPED: 'status-shipped',
      DELIVERED: 'status-delivered',      COMPLETED: 'status-completed',
      CANCELLED: 'status-cancelled',      REFUNDED: 'status-refunded'
    };
    return map[status] ?? '';
  }

  statusLabel(status: OrderStatus): string { return status.replace(/_/g, ' '); }
}