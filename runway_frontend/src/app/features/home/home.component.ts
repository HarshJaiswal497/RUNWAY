import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CatalogService } from '../../core/services/catalog.service';
import { ProductResponse, CategoryResponse, MediaResponse } from '../../core/models/product.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  private catalog = inject(CatalogService);
  private router = inject(Router);

  // ── Signals ───────────────────────────────
  products = signal<ProductResponse[]>([]);
  categories = signal<CategoryResponse[]>([]);
  loadingProducts = signal(true);
  loadingCategories = signal(true);
  error = signal<string | null>(null);

  // ── Computed ──────────────────────────────
  newArrivals = computed(() => this.products().slice(0, 4));

  // ── Category image map keyed by slug ──────────────
  private categoryImageMap: Record<string, string> = {
    'apparel': 'categories/apparel.png',
    'accessories': 'categories/accessories.png',
    'footwear': 'categories/footwear.png',
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.catalog.getAllRootCategories().subscribe({
      next: (data) => {
        this.categories.set(data);
        this.loadingCategories.set(false);
      },
      error: () => {
        this.loadingCategories.set(false);
      }
    });
  }

  loadProducts(): void {
    this.catalog.getAllProducts().subscribe({
      next: (data) => {
        this.products.set([...data].reverse()); 
        this.loadingProducts.set(false);
      },
      error: () => {
        this.error.set('Failed to load products.');
        this.loadingProducts.set(false);
      }
    });
  }

  getCategoryImage(slug: string): string {
    return this.categoryImageMap[slug] ?? 'categories/default.jpg';
  }

  getPrimaryImage(media: MediaResponse[]): string {
    if (!media || media.length === 0) return 'placeholder.jpg';
    const primary = media.find(m => m.isPrimary);
    return primary ? primary.fullUrl : media[0].fullUrl;
  }

  goToCategory(id: number): void {
    this.router.navigate(['/category', id]);
  }

  goToProduct(id: number): void {
    this.router.navigate(['/product', id]);
  }

  goToAllProducts(): void {
  this.router.navigate(['/category', 'new']);
}
}