import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { PaymentService } from '../../core/services/payment.service';
import { OrderService } from '../../core/services/order.service';
import { OrderResponse, PaymentIntentResponse } from '../../core/models/order.model';

export type RetryStep = 'confirm' | 'processing' | 'success' | 'failed';

@Component({
  selector: 'app-payment-retry',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './payment-retry.component.html',
  styleUrl: './payment-retry.component.css'
})
export class PaymentRetryComponent implements OnInit {
  private auth       = inject(AuthService);
  private paymentSvc = inject(PaymentService);
  private orderSvc   = inject(OrderService);
  private route      = inject(ActivatedRoute);
  private router     = inject(Router);

  loading       = signal(true);
  error         = signal<string | null>(null);
  order         = signal<OrderResponse | null>(null);
  paymentIntent = signal<PaymentIntentResponse | null>(null);
  step          = signal<RetryStep>('confirm');
  orderId       = signal<number>(0);

  // Only PENDING_PAYMENT orders can be retried
  canRetry = computed(() =>
    this.order()?.status === 'PENDING_PAYMENT'
  );

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }

    const orderId = Number(this.route.snapshot.paramMap.get('id'));
    if (!orderId) { this.router.navigate(['/orders']); return; }
    this.orderId.set(orderId);

    this.orderSvc.getOrderById(orderId).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
        // If order is already paid, redirect to order detail
        if (o.status !== 'PENDING_PAYMENT') {
          this.router.navigate(['/orders', orderId]);
        }
      },
      error: () => {
        this.error.set('Order not found.');
        this.loading.set(false);
      }
    });
  }

  retryPayment(): void {
    const order = this.order();
    if (!order) return;

    this.step.set('processing');
    this.error.set(null);

    // Create a new payment intent with order's totalAmount
    this.paymentSvc.createIntent(order.id, order.totalAmount).subscribe({
      next: (intent) => {
        this.paymentIntent.set(intent);
        // Simulate payment confirmation — replace with real gateway
        this.confirmPayment(order.id);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Could not initiate payment. Please try again.');
        this.step.set('confirm');
      }
    });
  }

  private confirmPayment(orderId: number): void {
    this.paymentSvc.updateStatus(orderId, 'COMPLETED').subscribe({
      next: (intent) => {
        this.paymentIntent.set(intent);
        this.step.set('success');
      },
      error: () => {
        this.paymentSvc.updateStatus(orderId, 'FAILED').subscribe();
        this.step.set('failed');
      }
    });
  }

  goToOrders(): void { this.router.navigate(['/orders']); }
  goToOrder(): void  { this.router.navigate(['/orders', this.orderId()]); }
}