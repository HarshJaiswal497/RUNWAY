import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { PaymentService } from '../../core/services/payment.service';
import { OrderService } from '../../core/services/order.service';
import { PaymentIntentResponse, PaymentStatus, OrderResponse } from '../../core/models/order.model';

@Component({
  selector: 'app-payment-status',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './payment-status.component.html',
  styleUrl: './payment-status.component.css'
})
export class PaymentStatusComponent implements OnInit {
  private auth       = inject(AuthService);
  private paymentSvc = inject(PaymentService);
  private orderSvc   = inject(OrderService);
  private route      = inject(ActivatedRoute);
  private router     = inject(Router);

  loading = signal(true);
  error   = signal<string | null>(null);
  payment = signal<PaymentIntentResponse | null>(null);
  order   = signal<OrderResponse | null>(null);
  orderId = signal<number>(0);

  statusIcon = computed(() => {
    switch (this.payment()?.status) {
      case 'COMPLETED': return '✓';
      case 'FAILED':    return '✕';
      case 'REFUNDED':  return '↩';
      default:          return '◌';
    }
  });

  statusClass = computed(() => {
    const map: Record<PaymentStatus, string> = {
      PENDING:   'status-pending',
      COMPLETED: 'status-completed',
      FAILED:    'status-failed',
      REFUNDED:  'status-refunded'
    };
    return map[this.payment()?.status ?? 'PENDING'] ?? '';
  });

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }

    const orderId = Number(this.route.snapshot.paramMap.get('id'));
    if (!orderId) { this.router.navigate(['/orders']); return; }
    this.orderId.set(orderId);

    // Load order details first (always available)
    this.orderSvc.getOrderById(orderId).subscribe({
      next: (o) => {
        this.order.set(o);
        // Then try to load payment intent (endpoint may not exist yet)
        this.paymentSvc.getByOrderId(orderId).subscribe({
          next:  (p) => { this.payment.set(p); this.loading.set(false); },
          error: ()  => {
            // Payment GET endpoint not available — show order status only
            this.loading.set(false);
          }
        });
      },
      error: () => {
        this.error.set('Order not found.');
        this.loading.set(false);
      }
    });
  }

  retryPayment(): void {
    this.router.navigate(['/orders', this.orderId(), 'payment']);
  }

  statusLabel(status: string): string {
    return status.replace(/_/g, ' ');
  }
}