import { Component, inject, OnInit, signal, computed, effect, HostListener } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { CatalogService } from '../../core/services/catalog.service';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { NotificationService } from '../../core/services/notification.service';
import { CategoryResponse, ProductResponse } from '../../core/models/product.model';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { getEventLabel } from '../../core/utils/notification.utils';
import { TooltipDirective } from '../tooltip/tooltip.directive';

export interface Designer {
  id: number;
  name: string;
  focus: string;
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, DecimalPipe, TooltipDirective],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class NavbarComponent implements OnInit {
  private catalog           = inject(CatalogService);
  readonly cartService      = inject(CartService);
  private router            = inject(Router);
  readonly authService      = inject(AuthService);
  readonly notifService     = inject(NotificationService);

  // ── Desktop panel state ───────────────────────────────────────────────────
  isMenuOpen        = signal(false);
  isDesignerOpen    = signal(false);
  isNotifOpen       = signal(false);
  isAccountMenuOpen = signal(false);
  isSearchOpen      = signal(false);

  // ── Active URL tracking ───────────────────────────────────────────────────
  currentUrl = signal(this.router.url);

  isActive(path: string): boolean {
    return this.currentUrl().startsWith(path);
  }

  // ── Mobile menu state ─────────────────────────────────────────────────────
  isMobileMenuOpen  = signal(false);
  mobileCatExpanded = signal(false);
  mobileDsnExpanded = signal(false);

  // ── Data ──────────────────────────────────────────────────────────────────
  allCategories    = signal<CategoryResponse[]>([]);
  designers        = signal<Designer[]>([]);
  loadingDesigners = signal(true);
  allProducts      = signal<ProductResponse[]>([]);
  searchQuery      = signal('');
  suggestions      = signal<ProductResponse[]>([]);
  private searchSubject = new Subject<string>();

  private readonly focusMap: Record<number, string> = {
    9:  'Accessories',
    10: 'Accessories',
    11: "Men's Footwear",
    12: "Women's Footwear",
    13: "Men's & Unisex Apparel",
    14: "Women's Apparel",
  };

  groupedCategories = computed(() => {
    const all     = this.allCategories();
    const parents = all.filter(c => !c.parentId || c.parentId === 0);
    return parents.map(parent => ({
      parent,
      children: all.filter(c => c.parentId === parent.id)
    }));
  });

  constructor() {
    effect(() => {
      const userId = this.authService.userId();
      if (userId) {
        this.cartService.getCart().subscribe({ error: () => {} });
        this.notifService.startPolling();
      } else {
        this.cartService.cartCount.set(0);
        this.notifService.stopPolling();
      }
    });

    // Keep currentUrl fresh on every navigation
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe(e => this.currentUrl.set((e as NavigationEnd).urlAfterRedirects));

    this.searchSubject.pipe(
      debounceTime(250),
      distinctUntilChanged()
    ).subscribe(query => {
      this.searchQuery.set(query);
      if (query.trim().length < 2) { this.suggestions.set([]); return; }
      const q = query.toLowerCase();
      this.suggestions.set(
        this.allProducts().filter(p => p.title.toLowerCase().includes(q)).slice(0, 6)
      );
    });
  }

  ngOnInit(): void {
    this.catalog.getAllCategories().subscribe({
      next:  d  => this.allCategories.set(d),
      error: () => {}
    });

    // Single load — feeds both search suggestions and designer list
    this.catalog.getAllProducts().subscribe({
      next: products => {
        this.allProducts.set(products);
        const ids = [...new Set(products.map(p => p.sellerId))];
        forkJoin(ids.map(id => this.authService.getUserDetails(id))).subscribe({
          next: responses => {
            this.designers.set(responses.map((res, i) => ({
              id:    ids[i],
              name:  res.user.name,
              focus: this.focusMap[ids[i]] ?? 'Designer'
            })));
            this.loadingDesigners.set(false);
          },
          error: () => this.loadingDesigners.set(false)
        });
      },
      error: () => this.loadingDesigners.set(false)
    });
  }

  // ── Search ────────────────────────────────────────────────────────────────
  toggleSearch(): void {
    this.isSearchOpen.update(v => !v);
    if (this.isSearchOpen()) {
      this.isMenuOpen.set(false);
      this.isDesignerOpen.set(false);
      this.isAccountMenuOpen.set(false);
      this.isNotifOpen.set(false);
      setTimeout(() => (document.querySelector('.search-box-input') as HTMLInputElement)?.focus(), 50);
    }
  }

  onSearchInput(value: string): void { this.searchSubject.next(value); }

  selectSuggestion(product: ProductResponse): void {
    this.router.navigate(['/product', product.id]);
    this.suggestions.set([]);
    this.searchQuery.set('');
    this.isSearchOpen.set(false);
  }

  viewAllResults(): void {
    const q = this.searchQuery().trim();
    if (q) {
      this.router.navigate(['/search'], { queryParams: { q } });
      this.suggestions.set([]);
      this.searchQuery.set('');
      this.isSearchOpen.set(false);
    }
  }

  clearSuggestions(): void { setTimeout(() => this.suggestions.set([]), 150); }

  onSearch(event: KeyboardEvent): void {
    if (event.key === 'Escape') { this.isSearchOpen.set(false); this.suggestions.set([]); return; }
    if (event.key === 'Enter') {
      const input = event.target as HTMLInputElement;
      const query = input.value.trim();
      if (query) {
        this.router.navigate(['/search'], { queryParams: { q: query } });
        input.value = '';
        this.suggestions.set([]);
        this.searchQuery.set('');
        this.closeAll();
      }
    }
  }

  // ── Desktop drawers ───────────────────────────────────────────────────────
  toggleMenu(): void {
    this.isMenuOpen.update(v => !v);
    if (this.isDesignerOpen()) this.isDesignerOpen.set(false);
    if (this.isSearchOpen())   this.isSearchOpen.set(false);
  }

  toggleDesigners(): void {
    this.isDesignerOpen.update(v => !v);
    if (this.isMenuOpen())   this.isMenuOpen.set(false);
    if (this.isSearchOpen()) this.isSearchOpen.set(false);
  }

  closeAll(): void {
    this.isMenuOpen.set(false);
    this.isDesignerOpen.set(false);
    this.isSearchOpen.set(false);
    this.isAccountMenuOpen.set(false);
    this.isNotifOpen.set(false);
  }

  goToCategory(id: number): void { this.router.navigate(['/category', id]); this.closeAll(); }
  goToDesigner(id: number): void  { this.router.navigate(['/category', 'seller', id]); this.closeAll(); }
  goToCart(): void { this.router.navigate(['/cart']); }

  // ── Mobile menu ───────────────────────────────────────────────────────────
  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
    document.body.style.overflow = this.isMobileMenuOpen() ? 'hidden' : '';
    if (!this.isMobileMenuOpen()) this.resetMobileMenu();
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
    document.body.style.overflow = '';
    this.resetMobileMenu();
  }

  private resetMobileMenu(): void {
    this.mobileCatExpanded.set(false);
    this.mobileDsnExpanded.set(false);
  }

  mobileNavigate(commands: any[]): void {
    this.router.navigate(commands);
    this.closeMobileMenu();
  }

  // ── Notifications ─────────────────────────────────────────────────────────
  toggleNotif(): void {
    this.isNotifOpen.update(v => !v);
    if (this.isAccountMenuOpen()) this.isAccountMenuOpen.set(false);
    if (this.isMenuOpen())        this.isMenuOpen.set(false);
    if (this.isDesignerOpen())    this.isDesignerOpen.set(false);
    if (this.isSearchOpen())      this.isSearchOpen.set(false);
    if (this.isNotifOpen())       this.notifService.load();
  }

  viewAllNotifications(): void {
    this.isNotifOpen.set(false);
    this.router.navigate(['/notifications']);
  }

  getEventLabel(event: string): string { return getEventLabel(event); }

  get latestNotifications() {
    return this.notifService.notifications()
      .slice()
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5);
  }

  timeAgo(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr.includes('Z') || dateStr.includes('+') ? dateStr : dateStr + 'Z');
    if (isNaN(d.getTime())) return '';
    const diff  = Date.now() - d.getTime();
    const mins  = Math.floor(diff / 60_000);
    const hours = Math.floor(diff / 3_600_000);
    const days  = Math.floor(diff / 86_400_000);
    if (mins  < 1)  return 'Just now';
    if (mins  < 60) return `${mins}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days  < 7)  return `${days}d ago`;
    return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', timeZone: 'Asia/Kolkata' });
  }

  // ── Account ───────────────────────────────────────────────────────────────
  toggleAccountMenu(): void { this.isAccountMenuOpen.update(v => !v); }

  goToLogin(): void {
    if (this.authService.isLoggedIn()) {
      this.toggleAccountMenu();
    } else {
      this.router.navigate(['/login']);
    }
  }

  formatRole(role: string | undefined): string {
    const map: Record<string, string> = {
      CUSTOMER: 'Valued Client',
      SELLER:   'Design House',
      ADMIN:    'RunWay Staff',
      SUPPORT:  'Client Advisor',
    };
    return map[role ?? ''] ?? (role ?? '');
  }

  goToAccount(): void { this.isAccountMenuOpen.set(false); this.router.navigate(['/account']); }
  goToSupport(): void { this.isAccountMenuOpen.set(false); this.router.navigate(['/support']); }

  logout(): void {
    this.authService.logout();
    this.isAccountMenuOpen.set(false);
  }

  // ── Global click — close dropdowns when clicking outside ──────────────────
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.account-wrap')) this.isAccountMenuOpen.set(false);
    if (!target.closest('.notif-wrap'))   this.isNotifOpen.set(false);
    if (!target.closest('.search-wrap'))  this.isSearchOpen.set(false);
  }
}
