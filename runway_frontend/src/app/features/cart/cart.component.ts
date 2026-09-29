import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { ProductImageService } from '../../core/services/product-image.service';
import { CartResponse, CartItemResponse } from '../../core/models/order.model';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css'
})
export class CartComponent implements OnInit {
  private auth     = inject(AuthService);
  private cartSvc  = inject(CartService);
  private imageSvc = inject(ProductImageService);
  private router   = inject(Router);

  loading     = signal(true);
  error       = signal<string | null>(null);
  cart        = signal<CartResponse | null>(null);
  removingIds = signal<Set<number>>(new Set());
  imageMap    = signal<Map<number, string>>(new Map());

  isEmpty = computed(() =>
    !this.loading() && (this.cart()?.items?.length ?? 0) === 0
  );

  subtotal(item: CartItemResponse): number {
    return item.unitPrice * item.quantity;
  }

  getImage(productId: number): string {
    return this.imageMap().get(productId) ?? '/placeholder.jpg';
  }

  ngOnInit(): void { this.loadCart(); }

  loadCart(): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }

    this.loading.set(true);
    this.error.set(null);

    this.cartSvc.getCart().subscribe({
      next: (c) => {
        this.cart.set(c);
        this.loading.set(false);
        this.loadImages(c);
      },
      error: (err) => {
        // 404 / 204 = new user with no cart yet — show empty state not error
        if (err?.status === 404 || err?.status === 204) {
          this.cart.set({ items: [], total: 0, totalItems: 0 } as any);
          this.error.set(null);
        } else {
          this.error.set('Failed to load your cart. Please try again.');
        }
        this.loading.set(false);
      }
    });
  }

  private loadImages(cart: CartResponse): void {
    if (!cart.items || cart.items.length === 0) return;
    this.imageSvc.getImageUrls(cart.items.map(i => i.productId)).subscribe(map => {
      this.imageMap.set(map);
    });
  }

  updateQuantity(productId: number, newQty: number): void {
    if (!this.auth.isLoggedIn()) return;
    if (newQty < 1) { this.remove(productId); return; }

    this.cartSvc.updateItem(productId, newQty).subscribe({
      next: (c) => this.cart.set(c)
    });
  }

  remove(productId: number): void {
    if (!this.auth.isLoggedIn()) return;

    const ids = new Set(this.removingIds());
    ids.add(productId);
    this.removingIds.set(ids);

    this.cartSvc.removeItem(productId).subscribe({
      next: (c) => {
        this.cart.set(c);
        const updated = new Map(this.imageMap());
        updated.delete(productId);
        this.imageMap.set(updated);
        const r = new Set(this.removingIds());
        r.delete(productId);
        this.removingIds.set(r);
      }
    });
  }

  checkout(): void { this.router.navigate(['/checkout']); }
  continueShopping(): void { this.router.navigate(['/category/all']); }
}