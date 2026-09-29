
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from './api.service';
import {
  ProductResponse,
  CategoryResponse,
  InventoryResponse
} from '../models/product.model';

import { HttpClient } from '@angular/common/http';


@Injectable({ providedIn: 'root' })
export class CatalogService {
  private api = inject(ApiService);

  // ── Products ──────────────────────────────────────────────

  getAllProducts(): Observable<ProductResponse[]> {
    return this.api.get<ProductResponse[]>('/catalog/products');
  }

  getProductById(id: number): Observable<ProductResponse> {
    return this.api.get<ProductResponse>(`/catalog/products/${id}`);
  }

  getProductsByCategory(categoryId: number): Observable<ProductResponse[]> {
    return this.api.get<ProductResponse[]>(`/catalog/products/category/${categoryId}`);
  }

  getProductsBySeller(sellerId: number): Observable<ProductResponse[]> {
    return this.api.get<ProductResponse[]>(`/catalog/products/seller/${sellerId}`);
  }

  // ── Categories ────────────────────────────────────────────

  getAllRootCategories(): Observable<CategoryResponse[]> {
    return this.api.get<CategoryResponse[]>('/catalog/categories');
  }
  getAllCategories(): Observable<CategoryResponse[]> {
    return this.api.get<CategoryResponse[]>('/catalog/categories/all');
  }

  // Get subcategories by parent id
  getSubcategories(parentId: number): Observable<CategoryResponse[]> {
    return this.api.get<CategoryResponse[]>('/catalog/categories').pipe(
      map((cats: CategoryResponse[]) => cats.filter((c: CategoryResponse) => c.parentId === parentId))
    );
  }

  // ── Inventory ─────────────────────────────────────────────

  getInventory(productId: number): Observable<InventoryResponse> {
    return this.api.get<InventoryResponse>(`/catalog/inventory/${productId}`);
  }

  // Seller Details

  getSellerDetails(userId: number): Observable<{ id: number; username: string; email: string }> {
    return this.api.get(`/auth/userdetails`, { userId });
  }

  // Wishlist
  addToWishlist(productId: number): Observable<void> {
    return this.api.post<void>(`/catalog/wishlist/${productId}`, {});
  }

  removeFromWishlist(productId: number): Observable<void> {
    return this.api.delete<void>(`/catalog/wishlist/${productId}`);
  }

  getWishlist(): Observable<ProductResponse[]> {
    return this.api.get<ProductResponse[]>('/catalog/wishlist');
  }

  checkWishlisted(productId: number): Observable<boolean> {
    return this.api.get<boolean>(`/catalog/wishlist/${productId}/check`);
  }
}