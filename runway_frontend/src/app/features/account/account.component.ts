import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { ProductImageService } from '../../core/services/product-image.service';
import { OrderResponse, OrderLineResponse } from '../../core/models/order.model';

export type AccountSection = 'overview' | 'orders' | 'delivered' | 'addresses' | 'settings';

export interface DeliveredItem {
  line:     OrderLineResponse;
  order:    OrderResponse;
  imageUrl: string;
}

export interface SavedAddress {
  id:        string;
  label:     string;
  address:   string;
  isDefault: boolean;
}

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './account.component.html',
  styleUrl: './account.component.css'
})
export class AccountComponent implements OnInit {
  private auth     = inject(AuthService);
  private orderSvc = inject(OrderService);
  private imageSvc = inject(ProductImageService);
  private router   = inject(Router);

  // ── Sidebar ───────────────────────────────────────────────────────────────
  sidebarOpen   = signal(true);
  activeSection = signal<AccountSection>('overview');

  readonly navItems: { key: AccountSection; label: string }[] = [
    { key: 'overview',  label: 'Overview'  },
    { key: 'orders',    label: 'My Orders' },
    { key: 'delivered', label: 'Delivered' },
    { key: 'addresses', label: 'Addresses' },
    { key: 'settings',  label: 'Account'   },
  ];

  // ── User ──────────────────────────────────────────────────────────────────
  userName    = signal('');
  userDetails = signal<any>(null);
  loadingUser = signal(true);

  // ── localStorage key scoped per user — prevents address sharing ──────────
  // Key format: "runway_addresses_<userId>"
  private get storageKey(): string {
    return `runway_addresses_${this.auth.userId() ?? 'guest'}`;
  }

  // ── Stats ─────────────────────────────────────────────────────────────────
  totalOrderCount  = signal(0);
  deliveredCount   = signal(0);
  loadingRecent    = signal(true);
  recentOrders     = signal<OrderResponse[]>([]);
  statsLoaded      = false;

  // ── Orders section ────────────────────────────────────────────────────────
  allOrders        = signal<OrderResponse[]>([]);
  loadingOrders    = signal(false);
  ordersPage       = signal(0);
  ordersTotalPages = signal(1);
  ordersLoaded     = false;
  readonly PAGE_SIZE = 10;

  // ── Delivered items ───────────────────────────────────────────────────────
  deliveredItems   = signal<DeliveredItem[]>([]);
  loadingDelivered = signal(false);
  deliveredLoaded  = false;

  // ── Addresses ─────────────────────────────────────────────────────────────
  addresses       = signal<SavedAddress[]>([]);
  showAddressForm = signal(false);
  editingAddress  = signal<SavedAddress | null>(null);
  addressError    = signal<string | null>(null);
  readonly MAX_ADDRESSES = 3;

  newLabel   = '';
  newAddress = '';
  newDefault = false;


  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }
    const user = this.auth.currentUser();
    this.userName.set(user?.username ?? 'Member');
    this.loadUserDetails();
    this.loadStats();
    this.loadAddresses();
  }

  toggleSidebar(): void { this.sidebarOpen.update(v => !v); }

  setSection(section: AccountSection): void {
    this.activeSection.set(section);
    if (section === 'orders'    && !this.ordersLoaded)    this.loadOrders(0);
    if (section === 'delivered' && !this.deliveredLoaded) this.loadDeliveredItems();
  }

  // ── User details ──────────────────────────────────────────────────────────
  private loadUserDetails(): void {
    const userId = this.auth.userId();
    if (!userId) return;
    this.auth.getUserDetails(userId).subscribe({
      next: (res) => { this.userDetails.set(res.user); this.loadingUser.set(false); },
      error: ()    => this.loadingUser.set(false)
    });
  }

  getUserRole(): string {
    const roleMap: Record<string, string> = {
      CUSTOMER: 'Valued Client',
      SELLER:   'Design House',
      ADMIN:    'RunWay Staff',
      SUPPORT:  'Client Advisor',
    };
    const roles: string[] = this.userDetails()?.roles ?? [];
    if (roles.length === 0) return 'Valued Client';
    const raw = roles[0].replace('ROLE_', '');
    return roleMap[raw] ?? raw;
  }

  // ── Stats — loads all 100 orders for accurate counts ─────────────────────
  private loadStats(): void {
    this.loadingRecent.set(true);
    this.orderSvc.getMyOrders(0, 100).subscribe({
      next: (res) => {
        const all = res.content;
        this.totalOrderCount.set(res.totalElements);
        this.deliveredCount.set(
          all.filter(o => o.status === 'DELIVERED' || o.status === 'COMPLETED').length
        );
        this.recentOrders.set(all.slice(0, 5));
        this.loadingRecent.set(false);
        this.statsLoaded = true;
      },
      error: () => this.loadingRecent.set(false)
    });
  }

  // ── Orders ────────────────────────────────────────────────────────────────
  loadOrders(page: number): void {
    this.loadingOrders.set(true);
    this.orderSvc.getMyOrders(page, this.PAGE_SIZE).subscribe({
      next: (res) => {
        this.allOrders.set(res.content);
        this.ordersPage.set(res.number);
        this.ordersTotalPages.set(res.totalPages);
        this.loadingOrders.set(false);
        this.ordersLoaded = true;
      },
      error: () => this.loadingOrders.set(false)
    });
  }

  prevOrderPage(): void {
    if (this.ordersPage() > 0) this.loadOrders(this.ordersPage() - 1);
  }

  nextOrderPage(): void {
    if (this.ordersPage() < this.ordersTotalPages() - 1) {
      this.loadOrders(this.ordersPage() + 1);
    }
  }

  // ── Delivered items ───────────────────────────────────────────────────────
  private loadDeliveredItems(): void {
    this.loadingDelivered.set(true);
    this.orderSvc.getMyOrders(0, 100).subscribe({
      next: (res) => {
        const delivered = res.content.filter(
          o => o.status === 'DELIVERED' || o.status === 'COMPLETED'
        );
        const items: DeliveredItem[] = delivered.flatMap(order =>
          order.lines.map(line => ({ line, order, imageUrl: '/placeholder.jpg' }))
        );
        if (items.length === 0) {
          this.deliveredItems.set([]);
          this.loadingDelivered.set(false);
          this.deliveredLoaded = true;
          return;
        }
        const ids = [...new Set(items.map(i => i.line.productId))];
        this.imageSvc.getImageUrls(ids).subscribe(imageMap => {
          this.deliveredItems.set(
            items.map(i => ({
              ...i,
              imageUrl: imageMap.get(i.line.productId) ?? '/placeholder.jpg'
            }))
          );
          this.loadingDelivered.set(false);
          this.deliveredLoaded = true;
        });
      },
      error: () => { this.loadingDelivered.set(false); this.deliveredLoaded = true; }
    });
  }

  goToReview(item: DeliveredItem): void {
    this.router.navigate(['/product', item.line.productId]);
  }

  // ── Addresses — scoped per user via storageKey ────────────────────────────
  // Each user gets their own key: "runway_addresses_<userId>"
  // This prevents User A's addresses showing for User B
  private loadAddresses(): void {
    const stored = localStorage.getItem(this.storageKey);
    if (stored) {
      this.addresses.set(JSON.parse(stored));
      return;
    }

    // Seed from this user's own past orders
    this.orderSvc.getMyOrders(0, 5).subscribe({
      next: (res) => {
        const seen = new Set<string>();
        const fromOrders: SavedAddress[] = [];
        res.content.forEach((order, idx) => {
          if (order.shippingAddress && !seen.has(order.shippingAddress)
              && fromOrders.length < this.MAX_ADDRESSES) {
            seen.add(order.shippingAddress);
            fromOrders.push({
              id:        `order_${order.id}`,
              label:     idx === 0 ? 'Home' : `Address ${idx + 1}`,
              address:   order.shippingAddress,
              isDefault: idx === 0
            });
          }
        });
        if (fromOrders.length > 0) {
          this.addresses.set(fromOrders);
          this.persist();
        }
      }
    });
  }

  // Saves to user-scoped key
  private persist(): void {
    localStorage.setItem(this.storageKey, JSON.stringify(this.addresses()));
  }

  canAddAddress(): boolean { return this.addresses().length < this.MAX_ADDRESSES; }

  openAddressForm(address?: SavedAddress): void {
    this.addressError.set(null);
    if (address) {
      this.editingAddress.set(address);
      this.newLabel   = address.label;
      this.newAddress = address.address;
      this.newDefault = address.isDefault;
    } else {
      if (!this.canAddAddress()) {
        this.addressError.set(`Maximum ${this.MAX_ADDRESSES} addresses allowed.`);
        return;
      }
      this.editingAddress.set(null);
      this.newLabel   = '';
      this.newAddress = '';
      this.newDefault = this.addresses().length === 0;
    }
    this.showAddressForm.set(true);
  }

  saveAddress(): void {
    if (this.newAddress.trim().length < 10) return;
    const editing = this.editingAddress();
    let current   = [...this.addresses()];
    if (editing) {
      current = current.map(a =>
        a.id === editing.id
          ? { ...a, label: this.newLabel || 'Address', address: this.newAddress.trim(), isDefault: this.newDefault }
          : this.newDefault ? { ...a, isDefault: false } : a
      );
    } else {
      const newAddr: SavedAddress = {
        id:        `addr_${Date.now()}`,
        label:     this.newLabel || 'Address',
        address:   this.newAddress.trim(),
        isDefault: this.newDefault || current.length === 0
      };
      if (newAddr.isDefault) current = current.map(a => ({ ...a, isDefault: false }));
      current.push(newAddr);
    }
    this.addresses.set(current);
    this.persist();
    this.showAddressForm.set(false);
    this.editingAddress.set(null);
  }

  deleteAddress(id: string): void {
    this.addresses.set(this.addresses().filter(a => a.id !== id));
    this.persist();
  }

  setDefault(id: string): void {
    this.addresses.set(this.addresses().map(a => ({ ...a, isDefault: a.id === id })));
    this.persist();
  }

  cancelAddressForm(): void {
    this.showAddressForm.set(false);
    this.editingAddress.set(null);
    this.addressError.set(null);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING_PAYMENT: 'st-pending',  PAID: 'st-paid',
      PROCESSING: 'st-processing',    SHIPPED: 'st-shipped',
      DELIVERED: 'st-delivered',      COMPLETED: 'st-completed',
      CANCELLED: 'st-cancelled',      REFUNDED: 'st-refunded'
    };
    return map[status] ?? '';
  }

  statusLabel(s: string): string { return s.replace(/_/g, ' '); }

  logout():              void { this.auth.logout(); this.router.navigate(['/']); }
  goToOrders():          void { this.router.navigate(['/orders']); }
  goToOrder(id: number): void { this.router.navigate(['/orders', id]); }
}