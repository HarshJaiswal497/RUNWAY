import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { CatalogService } from './catalog.service';
import { MediaResponse } from '../models/product.model';

// ─────────────────────────────────────────────────────────────────────────────
// ProductImageService
//
// Fetches and caches primary product images from the Catalog Service.
// Used by Orders pages to enrich OrderLine.productId → image URL.
//
// The Order Service backend stores only productTitle and unitPrice snapshots
// in order_lines — no image URLs. We fetch them from:
//   GET /catalog/products/{id}  → ProductResponse.media[]
//
// Results are cached in-memory for the session lifetime so navigating
// between order detail and order list doesn't re-fetch the same product.
//
// Returns '/placeholder.jpg' on any error so the UI always shows something.
// ─────────────────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class ProductImageService {
  private catalog = inject(CatalogService);

  // In-memory cache: productId → primary image URL
  private cache = new Map<number, string>();

  // Get primary image URL for a product — returns cached value if available
  getImageUrl(productId: number): Observable<string> {
    if (this.cache.has(productId)) {
      return of(this.cache.get(productId)!);
    }

    return this.catalog.getProductById(productId).pipe(
      map(product => this.extractPrimary(product.media)),
      tap(url => this.cache.set(productId, url))
    );
  }

  // Batch-fetch image URLs for multiple productIds.
  // Returns a Map<productId, imageUrl> when all are resolved.
  // Uses cache — only fetches IDs not already in cache.
  getImageUrls(productIds: number[]): Observable<Map<number, string>> {
    const result = new Map<number, string>();
    const toFetch = productIds.filter(id => !this.cache.has(id));

    // Pre-fill from cache
    productIds.forEach(id => {
      if (this.cache.has(id)) result.set(id, this.cache.get(id)!);
    });

    if (toFetch.length === 0) {
      return of(result);
    }

    // Fetch uncached products — resolve all then return combined map
    return new Observable(observer => {
      let completed = 0;

      toFetch.forEach(id => {
        this.catalog.getProductById(id).subscribe({
          next: (product) => {
            const url = this.extractPrimary(product.media);
            this.cache.set(id, url);
            result.set(id, url);
            completed++;
            if (completed === toFetch.length) {
              observer.next(new Map(result));
              observer.complete();
            }
          },
          error: () => {
            // Failed fetch — use placeholder, still count as completed
            const url = '/placeholder.jpg';
            this.cache.set(id, url);
            result.set(id, url);
            completed++;
            if (completed === toFetch.length) {
              observer.next(new Map(result));
              observer.complete();
            }
          }
        });
      });
    });
  }

  private extractPrimary(media: MediaResponse[]): string {
    if (!media || media.length === 0) return '/placeholder.jpg';
    return (media.find(m => m.isPrimary) ?? media[0]).fullUrl;
  }
}