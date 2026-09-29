import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import {
  ApiResponse,
  PagedData,
  OrderResponse,
  CheckoutRequest
} from '../models/order.model';


@Injectable({ providedIn: 'root' })
export class OrderService {
  private api = inject(ApiService);

  // POST /api/orders/checkout
  // Body: { shippingAddress, notes? }
  placeOrder(request: CheckoutRequest): Observable<OrderResponse> {
    return this.api
      .post<ApiResponse<OrderResponse>>('/api/orders/checkout', request)
      .pipe(map(r => r.data));
  }

  // GET /api/orders/my?page=0&size=8
  getMyOrders(page = 0, size = 8): Observable<PagedData<OrderResponse>> {
    return this.api
      .get<ApiResponse<PagedData<OrderResponse>>>('/api/orders/my', { page, size })
      .pipe(map(r => r.data));
  }

  // GET /api/orders/{id}
  getOrderById(orderId: number): Observable<OrderResponse> {
    return this.api
      .get<ApiResponse<OrderResponse>>(`/api/orders/${orderId}`)
      .pipe(map(r => r.data));
  }

  // PUT /api/orders/{id}/cancel
  cancelOrder(orderId: number): Observable<OrderResponse> {
    return this.api
      .put<ApiResponse<OrderResponse>>(`/api/orders/${orderId}/cancel`, {})
      .pipe(map(r => r.data));
  }
}