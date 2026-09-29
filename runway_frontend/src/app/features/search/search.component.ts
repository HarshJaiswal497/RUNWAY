import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { CatalogService } from '../../core/services/catalog.service';
import { ProductResponse, MediaResponse } from '../../core/models/product.model';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './search.component.html',
  styleUrl: './search.component.css'
})
export class SearchComponent implements OnInit {
  private route   = inject(ActivatedRoute);
   router  = inject(Router);
  private catalog = inject(CatalogService);

  query          = signal('');
  allProducts    = signal<ProductResponse[]>([]);
  loading        = signal(true);
  visibleCount   = signal(12);

  // ── Search across title, sku, description, material, color ──
  results = computed(() => {
    const q = this.query().toLowerCase().trim();
    if (!q) return [];

    return this.allProducts().filter(p => {
      return (
        p.title?.toLowerCase().includes(q) ||
        p.sku?.toLowerCase().includes(q) ||
        p.description?.toLowerCase().includes(q) ||
        p.attributes?.['material']?.toLowerCase().includes(q) ||
        p.attributes?.['color']?.toLowerCase().includes(q) ||
        p.attributes?.['gender']?.toLowerCase().includes(q) ||
        p.attributes?.['occasion']?.toLowerCase().includes(q)
      );
    });
  });

  visibleResults = computed(() =>
    this.results().slice(0, this.visibleCount())
  );

  hasMore = computed(() =>
    this.visibleCount() < this.results().length
  );

  ngOnInit(): void {
    window.scrollTo({ top: 0, behavior: 'instant' });

    // Listen for query param changes
    this.route.queryParams.subscribe(params => {
      const q = params['q'] ?? '';
      this.query.set(q);
      this.visibleCount.set(12);
      window.scrollTo({ top: 0, behavior: 'instant' });
    });

    // Load all products once
    this.catalog.getAllProducts().subscribe({
      next: (products) => {
        this.allProducts.set(products);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  loadMore(): void {
    this.visibleCount.update(n => n + 12);
  }

  goToProduct(id: number): void {
    this.router.navigate(['/product', id]);
  }

  getPrimaryImage(media: MediaResponse[]): string {
    if (!media || media.length === 0) return '/placeholder.jpg';
    const primary = media.find(m => m.isPrimary);
    return primary ? primary.fullUrl : media[0].fullUrl;
  }

  // Highlight matching text
  highlight(text: string): string {
    if (!text) return '';
    const q = this.query().trim();
    if (!q) return text;
    const regex = new RegExp(`(${q})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  }
}