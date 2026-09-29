import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import {
  ApiResponse,
  PaymentIntentResponse,
  PaymentStatus
} from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private api  = inject(ApiService);
  private auth = inject(AuthService);
  private http = inject(HttpClient);

  // POST /payment/{orderId}/intent
  // amount sent as "86000.00" string to match Java BigDecimal scale
  createIntent(
    orderId: number,
    amount: number | string
  ): Observable<PaymentIntentResponse> {
    const body = { amount: parseFloat(String(amount)).toFixed(2) };
    return this.api
      .post<ApiResponse<PaymentIntentResponse>>(`/payment/${orderId}/intent`, body)
      .pipe(map(r => r.data));
  }

  // PUT /payment/{orderId}/status — admin only (for actual ADMIN users)
  updateStatus(
    orderId: number,
    status: PaymentStatus
  ): Observable<PaymentIntentResponse> {
    return this.api
      .put<ApiResponse<PaymentIntentResponse>>(
        `/payment/${orderId}/status`,
        { status }
      )
      .pipe(map(r => r.data));
  }

  // PUT /payment/{orderId}/confirm — new customer-accessible endpoint
  // Added by backend teammate — no role check, only requires X-User-Id
  // Called after customer completes mock payment form
  confirmPayment(orderId: number): Observable<PaymentIntentResponse> {
    const token  = this.auth.getToken();
    const userId = this.auth.userId();

    // Only Authorization + X-User-Id needed — no X-User-Role required
    const headers = new HttpHeaders()
      .set('Content-Type',  'application/json')
      .set('Authorization', `Bearer ${token ?? ''}`)
      .set('X-User-Id',     String(userId ?? ''));

    const url = `${this.api.baseUrl}/payment/${orderId}/confirm`;

    console.log('[PaymentService] confirmPayment — orderId:', orderId,
      'X-User-Id:', userId, 'url:', url);

    return this.http
      .put<ApiResponse<PaymentIntentResponse>>(url, {}, { headers })
      .pipe(map(r => r.data));
  }

  // GET /payment/{orderId} — optional endpoint
  getByOrderId(orderId: number): Observable<PaymentIntentResponse> {
    return this.api
      .get<ApiResponse<PaymentIntentResponse>>(`/payment/${orderId}`)
      .pipe(map(r => r.data));
  }
}