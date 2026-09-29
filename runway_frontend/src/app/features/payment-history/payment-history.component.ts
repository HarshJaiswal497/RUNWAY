import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { OrderResponse, OrderStatus } from '../../core/models/order.model';

// Payment history is derived from orders since the Payment Service
// does not expose a GET /payment/my endpoint in the current controller.
// Each order represents one payment transaction.
export interface PaymentHistoryRow {
  orderId: number;
  orderNumber: string;
  amount: number;
  currency: string;
  orderStatus: OrderStatus;
  // Derived payment status from order status
  paymentStatus: 'COMPLETED' | 'PENDING' | 'FAILED' | 'REFUNDED';
  date: string;
  itemCount: number;
}

@Component({
  selector: 'app-payment-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './payment-history.component.html',
  styleUrl: './payment-history.component.css'
})
export class PaymentHistoryComponent implements OnInit {
  private auth     = inject(AuthService);
  private orderSvc = inject(OrderService);
  private router   = inject(Router);

  loading     = signal(true);
  error       = signal<string | null>(null);
  rows        = signal<PaymentHistoryRow[]>([]);
  currentPage = signal(0);
  totalPages  = signal(1);
  totalItems  = signal(0);
  readonly pageSize = 10;

  isEmpty = computed(() => !this.loading() && this.rows().length === 0);

  // Summary computed signals
  totalSpent = computed(() =>
    this.rows()
      .filter(r => r.paymentStatus === 'COMPLETED')
      .reduce((sum, r) => sum + r.amount, 0)
  );

  completedCount  = computed(() => this.rows().filter(r => r.paymentStatus === 'COMPLETED').length);
  pendingCount    = computed(() => this.rows().filter(r => r.paymentStatus === 'PENDING').length);
  failedCount     = computed(() => this.rows().filter(r => r.paymentStatus === 'FAILED').length);

  ngOnInit(): void { this.loadHistory(0); }

  loadHistory(page: number): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }

    this.loading.set(true);
    this.error.set(null);

    this.orderSvc.getMyOrders(page, this.pageSize).subscribe({
      next: (res) => {
        this.currentPage.set(res.number);
        this.totalPages.set(res.totalPages);
        this.totalItems.set(res.totalElements);
        this.rows.set(res.content.map(o => this.toRow(o)));
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Failed to load payment history.');
        this.loading.set(false);
      }
    });
  }

  private toRow(order: OrderResponse): PaymentHistoryRow {
    return {
      orderId:       order.id,
      orderNumber:   order.orderNumber,
      amount:        order.totalAmount,
      currency:      order.currency,
      orderStatus:   order.status,
      paymentStatus: this.derivePaymentStatus(order.status),
      date:          order.createdAt,
      itemCount:     order.lines.length
    };
  }

  // Derive payment status from order status
  // (since no GET /payment/my endpoint exists in current controller)
  private derivePaymentStatus(
    status: OrderStatus
  ): 'COMPLETED' | 'PENDING' | 'FAILED' | 'REFUNDED' {
    switch (status) {
      case 'PENDING_PAYMENT': return 'PENDING';
      case 'CANCELLED':       return 'FAILED';
      case 'REFUNDED':        return 'REFUNDED';
      default:                return 'COMPLETED';
    }
  }

  payStatusClass(status: string): string {
    const map: Record<string, string> = {
      COMPLETED: 'pay-completed',
      PENDING:   'pay-pending',
      FAILED:    'pay-failed',
      REFUNDED:  'pay-refunded'
    };
    return map[status] ?? '';
  }

  prevPage(): void { if (this.currentPage() > 0) this.loadHistory(this.currentPage() - 1); }
  nextPage(): void { if (this.currentPage() < this.totalPages() - 1) this.loadHistory(this.currentPage() + 1); }

  goToOrder(orderId: number): void  { this.router.navigate(['/orders', orderId]); }
  retryPayment(orderId: number): void { this.router.navigate(['/orders', orderId, 'payment']); }
}