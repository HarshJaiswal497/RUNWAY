import { Component, inject, OnInit, signal, computed, effect, HostListener } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { CatalogService } from '../../core/services/catalog.service';
import { AuthService } from '../../core/services/auth.service';
import {
  ProductResponse,
  CategoryResponse,
  MediaResponse
} from '../../core/models/product.model';

@Component({
  selector: 'app-category',
  standalone: true,
  imports: [DecimalPipe, TitleCasePipe],
  templateUrl: './category.component.html',
  styleUrl: './category.component.css'
})
export class CategoryComponent implements OnInit {
  private catalog  = inject(CatalogService);
  private auth     = inject(AuthService);
  private route    = inject(ActivatedRoute);
  private router   = inject(Router);
  sortOpen = false;

  readonly sortOptions = [
    { value: 'newest', label: 'New In' },
    { value: 'price-asc', label: 'Price ↑' },
    { value: 'price-desc', label: 'Price ↓' },
  ];

  openFilters: Record<string, boolean> = {
    gender: false,
    price: false,
    material: false,
    color: false
  };

  readonly priceRanges = [
    { label: 'Under ₹10,000', min: null, max: 10000 },
    { label: '₹10,000 – ₹25,000', min: 10000, max: 25000 },
    { label: '₹25,000 – ₹50,000', min: 25000, max: 50000 },
    { label: '₹50,000 – ₹1,00,000', min: 50000, max: 100000 },
    { label: 'Above ₹1,00,000', min: 100000, max: null },
  ];

  // ── Raw data ──────────────────────────────────────
  allProducts = signal<ProductResponse[]>([]);
  allCategories = signal<CategoryResponse[]>([]);
  parentCategory = signal<CategoryResponse | null>(null);
  subcategories = signal<CategoryResponse[]>([]);

  // ── Loading / error ───────────────────────────────
  loadingProducts = signal(true);
  loadingCategories = signal(true);
  error = signal<string | null>(null);

  // ── Active filters ────────────────────────────────
  activeSubcategory = signal<number | null>(null);
  activeGender = signal<string | null>(null);
  activeMaterial = signal<string | null>(null);
  activeColor = signal<string | null>(null);
  priceMin = signal<number | null>(null);
  priceMax = signal<number | null>(null);
  sortBy = signal<string>('newest');

  // ── Display ───────────────────────────────────────
  visibleCount = signal<number>(9);
  sidebarOpen = signal<boolean>(false);

  // ── Computed: subcategory IDs under this parent ───
  subcategoryIds = computed(() => this.subcategories().map(s => s.id));

  // ── Computed: products belonging to this parent ───
  parentProducts = computed(() => {
    const subIds = this.subcategoryIds();
    const parentId = this.parentCategory()?.id;
    if (!parentId) return this.allProducts();
    return this.allProducts().filter(p =>
      p.categoryId === parentId || subIds.includes(p.categoryId)
    );
  });

  sortLabel = computed(() =>
    this.sortOptions.find(o => o.value === this.sortBy())?.label ?? 'New In'
  );
  //sort
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.sort-wrap')) {
      this.sortOpen = false;
    }
  }

  // toggle filter
  toggleFilter(key: string): void {
    this.openFilters[key] = !this.openFilters[key];
  }

  // ── Computed: available filter options ────────────
  availableGenders = computed(() => {
    const set = new Set<string>();
    this.parentProducts().forEach(p => {
      if (p.attributes?.['gender']) set.add(p.attributes['gender']);
    });
    return Array.from(set);
  });

  availableMaterials = computed(() => {
    const set = new Set<string>();
    this.parentProducts().forEach(p => {
      if (p.attributes?.['material']) set.add(p.attributes['material']);
    });
    return Array.from(set);
  });

  availableColors = computed(() => {
    const set = new Set<string>();
    this.parentProducts().forEach(p => {
      if (p.attributes?.['color']) set.add(p.attributes['color']);
    });
    this.parentProducts().forEach(p => console.log(JSON.stringify(p.attributes?.['gender'])));
    return Array.from(set);
  });

  // ── Computed: filtered + sorted products ──────────
  filteredProducts = computed(() => {
    let products = [...this.parentProducts()];
    // Subcategory filterS
    const subId = this.activeSubcategory();
    if (subId !== null) {
      products = products.filter(p => p.categoryId === subId);
    }

    // Gender filter
    const gender = this.activeGender();
    if (gender) {
      products = products.filter(p => p.attributes?.['gender'] === gender);
    }

    // Material filter
    const material = this.activeMaterial();
    if (material) {
      products = products.filter(p => p.attributes?.['material'] === material);
    }

    // Color filter
    const color = this.activeColor();
    if (color) {
      products = products.filter(p => p.attributes?.['color'] === color);
    }

    // Price filter
    const min = this.priceMin();
    const max = this.priceMax();
    if (min !== null) products = products.filter(p => p.price >= min);
    if (max !== null) products = products.filter(p => p.price <= max);

    // Sort
    switch (this.sortBy()) {
      case 'price-asc': products.sort((a, b) => a.price - b.price); break;
      case 'price-desc': products.sort((a, b) => b.price - a.price); break;
      case 'newest': products.reverse(); break;
    }

    return products;
  });

  // ── Computed: visible slice ────────────────────────
  visibleProducts = computed(() =>
    this.filteredProducts().slice(0, this.visibleCount())
  );

  hasMore = computed(() =>
    this.visibleCount() < this.filteredProducts().length
  );

  ngOnInit(): void {
    this.route.url.subscribe(segments => {
      const first = segments[1]?.path ?? '';
      const second = segments[2]?.path ?? '';

      this.resetFilters();
      this.visibleCount.set(9);
      window.scrollTo({ top: 0, behavior: 'instant' });

      if (first === 'all') {
        this.loadAllProducts();
      } else if (first === 'new') {
        this.loadNewIn();
      } else if (first === 'seller') {
        this.loadBySeller(Number(second));
      } else {
        this.loadData(Number(first));
      }
    });
  }
  private loadBySeller(sellerId: number): void {
    this.loadingCategories.set(true);
    this.loadingProducts.set(true);
    this.error.set(null);
    this.subcategories.set([]);

    // Fetch designer name from auth service (same source as the navbar)
    this.auth.getUserDetails(sellerId).subscribe({
      next: (res) => {
        this.parentCategory.set({
          id: 0,
          name: res.user.name,
          slug: `seller-${sellerId}`,
          description: '',
          parentId: null
        });
        this.loadingCategories.set(false);
      },
      error: () => {
        this.parentCategory.set({
          id: 0,
          name: 'Designer',
          slug: `seller-${sellerId}`,
          description: '',
          parentId: null
        });
        this.loadingCategories.set(false);
      }
    });

    this.catalog.getProductsBySeller(sellerId).subscribe({
      next: (products) => {
        this.allProducts.set(products);
        this.loadingProducts.set(false);
      },
      error: () => {
        this.error.set('Failed to load designer products.');
        this.loadingProducts.set(false);
      }
    });
  }

  private loadNewIn(): void {
    this.loadingCategories.set(false);
    this.loadingProducts.set(true);
    this.error.set(null);

    this.parentCategory.set({
      id: 0,
      name: 'New In',
      slug: 'new',
      description: 'The latest arrivals — fresh from our curated selection',
      parentId: null
    });
    console.log('parentCategory set:', this.parentCategory());
    console.log('loadingCategories:', this.loadingCategories());

    this.subcategories.set([]);
    this.sortBy.set('newest'); // ← force newest first

    this.catalog.getAllProducts().subscribe({
      next: (products) => {
        this.allProducts.set(products);
        this.loadingProducts.set(false);
      },
      error: () => {
        this.error.set('Failed to load products.');
        this.loadingProducts.set(false);
      }
    });
  }

  private loadAllProducts(): void {
    this.loadingCategories.set(false);
    this.loadingProducts.set(true);
    this.error.set(null);

    // Set a virtual "All" parent category for the header
    this.parentCategory.set({
      id: 0,
      name: 'All Pieces',
      slug: 'all',
      description: 'The complete RunWay collection',
      parentId: null
    });

    // No subcategories for "all"
    this.subcategories.set([]);

    this.catalog.getAllProducts().subscribe({
      next: (products) => {
        this.allProducts.set(products);
        this.loadingProducts.set(false);
      },
      error: () => {
        this.error.set('Failed to load products.');
        this.loadingProducts.set(false);
      }
    });
  }

  private loadData(parentId: number): void {
    this.loadingCategories.set(true);
    this.loadingProducts.set(true);
    this.error.set(null);

    this.catalog.getAllCategories().subscribe({
      next: (cats) => {
        this.allCategories.set(cats);
        const parent = cats.find(c => c.id === parentId) ?? null;
        this.parentCategory.set(parent);
        const subs = cats.filter(c => c.parentId === parentId);
        this.subcategories.set(subs);
        this.loadingCategories.set(false);

        this.catalog.getAllProducts().subscribe({
          next: (products) => {
            this.allProducts.set(products);
            this.loadingProducts.set(false);
          },
          error: () => {
            this.error.set('Failed to load products.');
            this.loadingProducts.set(false);
          }
        });
      },
      error: () => {
        this.error.set('Failed to load categories.');
        this.loadingCategories.set(false);
        this.loadingProducts.set(false);
      }
    });
  }

  // ── Filter actions ────────────────────────────────
  setSubcategory(id: number | null): void {
    this.activeSubcategory.set(id);
    this.visibleCount.set(9);
  }

  setGender(value: string): void {
    this.activeGender.set(this.activeGender() === value ? null : value);
    this.visibleCount.set(9);
  }

  setMaterial(value: string): void {
  this.activeMaterial.set(this.activeMaterial() === value ? null : value);
  this.visibleCount.set(9);
}

  setColor(value: string): void {
    this.activeColor.set(this.activeColor() === value ? null : value);
    this.visibleCount.set(9);
  }

  setPriceRange(min: number | null, max: number | null): void {
  const alreadyActive = this.priceMin() === min && this.priceMax() === max;
  this.priceMin.set(alreadyActive ? null : min);
  this.priceMax.set(alreadyActive ? null : max);
  this.visibleCount.set(9);
}

  setSort(value: string): void {
    this.sortBy.set(value);
  }

  resetFilters(): void {
    this.activeSubcategory.set(null);
    this.activeGender.set(null);
    this.activeMaterial.set(null);
    this.activeColor.set(null);
    this.priceMin.set(null);
    this.priceMax.set(null);
    this.sortBy.set('newest');
  }

  loadMore(): void {
    this.visibleCount.update(n => n + 6);
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  // ── Helpers ───────────────────────────────────────
  getPrimaryImage(media: MediaResponse[]): string {
    if (!media || media.length === 0) return '/placeholder.jpg';
    const primary = media.find(m => m.isPrimary);
    return primary ? primary.fullUrl : media[0].fullUrl;
  }

  goToProduct(id: number): void {
    this.router.navigate(['/product', id]);
  }

  activeFilterCount = computed(() => {
    let count = 0;
    if (this.activeSubcategory() !== null) count++;
    if (this.activeGender()) count++;
    if (this.activeMaterial()) count++;
    if (this.activeColor()) count++;
    if (this.priceMin() !== null || this.priceMax() !== null) count++;
    return count;
  });
}