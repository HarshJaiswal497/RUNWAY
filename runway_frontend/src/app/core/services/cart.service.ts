import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { ApiService } from './api.service';
import {
  ApiResponse,
  CartResponse,
  AddCartItemRequest,
  UpdateCartItemRequest
} from '../models/order.model';

// ─────────────────────────────────────────────────────────────────────────────
// CartService — shared cart state
//
// cartCount signal is the single source of truth for the navbar badge.
// It updates immediately after every addItem / removeItem / updateItem / clear.
// The navbar reads cartService.cartCount() directly — no polling needed.
// ─────────────────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class CartService {
  private api = inject(ApiService);

  // Shared signal — navbar badge reads this directly
  // Updated after every cart mutation so badge reflects immediately
  readonly cartCount = signal(0);

  // GET /api/cart/my
  getCart(): Observable<CartResponse> {
    return this.api
      .get<ApiResponse<CartResponse>>('/api/cart/my')
      .pipe(
        map(r => r.data),
        tap(cart => this.cartCount.set(cart.totalItems ?? 0))
      );
  }

  // POST /api/cart/my/items
  addItem(item: AddCartItemRequest): Observable<CartResponse> {
    return this.api
      .post<ApiResponse<CartResponse>>('/api/cart/my/items', item)
      .pipe(
        map(r => r.data),
        tap(cart => this.cartCount.set(cart.totalItems ?? 0))
      );
  }

  // PUT /api/cart/my/items/{productId}
  updateItem(productId: number, quantity: number): Observable<CartResponse> {
    const body: UpdateCartItemRequest = { quantity };
    return this.api
      .put<ApiResponse<CartResponse>>(`/api/cart/my/items/${productId}`, body)
      .pipe(
        map(r => r.data),
        tap(cart => this.cartCount.set(cart.totalItems ?? 0))
      );
  }

  // DELETE /api/cart/my/items/{productId}
  removeItem(productId: number): Observable<CartResponse> {
    return this.api
      .delete<ApiResponse<CartResponse>>(`/api/cart/my/items/${productId}`)
      .pipe(
        map(r => r.data),
        tap(cart => this.cartCount.set(cart.totalItems ?? 0))
      );
  }

  // DELETE /api/cart/my
  clearCart(): Observable<CartResponse> {
    return this.api
      .delete<ApiResponse<CartResponse>>('/api/cart/my')
      .pipe(
        map(r => r.data),
        tap(() => this.cartCount.set(0))
      );
  }
}