import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RwSelectComponent } from '../../shared/rw-select/rw-select.component';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { CartResponse, CheckoutRequest, PaymentIntentResponse } from '../../core/models/order.model';

export type CheckoutStep = 'review' | 'payment' | 'confirmed' | 'failed';
export type PayMethod    = 'card' | 'upi' | 'netbanking';

export interface SavedAddress {
  id:        string;
  label:     string;
  address:   string;
  isDefault: boolean;
}

// Individual field errors
export interface AddressFieldErrors {
  flatNo:  string | null;
  street:  string | null;
  area:    string | null;
  city:    string | null;
  state:   string | null;
  pinCode: string | null;
}

// Indian states list for validation
const INDIAN_STATES = [
  'Andhra Pradesh','Arunachal Pradesh','Assam','Bihar','Chhattisgarh',
  'Goa','Gujarat','Haryana','Himachal Pradesh','Jharkhand','Karnataka',
  'Kerala','Madhya Pradesh','Maharashtra','Manipur','Meghalaya','Mizoram',
  'Nagaland','Odisha','Punjab','Rajasthan','Sikkim','Tamil Nadu','Telangana',
  'Tripura','Uttar Pradesh','Uttarakhand','West Bengal',
  'Andaman and Nicobar Islands','Chandigarh','Dadra and Nagar Haveli',
  'Daman and Diu','Delhi','Jammu and Kashmir','Ladakh','Lakshadweep','Puducherry'
];

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, RwSelectComponent],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent implements OnInit {
  private auth       = inject(AuthService);
  private cartSvc    = inject(CartService);
  private orderSvc   = inject(OrderService);
  private paymentSvc = inject(PaymentService);
  private router     = inject(Router);

  step          = signal<CheckoutStep>('review');
  loading       = signal(true);
  processing    = signal(false);
  error         = signal<string | null>(null);
  cart          = signal<CartResponse | null>(null);
  orderId       = signal<number | null>(null);
  orderNumber   = signal<string>('');
  currency      = signal<string>('INR');
  paymentIntent = signal<PaymentIntentResponse | null>(null);

  notes = '';

  // ── Saved addresses ───────────────────────────────────────────────────────
  savedAddresses    = signal<SavedAddress[]>([]);
  selectedAddressId = signal<string>('');
  shippingAddress   = '';   // final composed string sent to backend
  manualEntry       = signal(false);

  // ── Separate address fields ───────────────────────────────────────────────
  flatNo  = signal('');   // Flat / House / Door No
  street  = signal('');   // Street / Road name
  area    = signal('');   // Area / Locality / Landmark
  city    = signal('');   // City / Town
  state   = signal('');   // State
  pinCode = signal('');   // 6-digit PIN

  // Track which fields have been touched (blur) for showing errors
  touched = signal<Set<string>>(new Set());

  touch(field: string): void {
    const s = new Set(this.touched());
    s.add(field);
    this.touched.set(s);
  }

  isTouched(field: string): boolean {
    return this.touched().has(field);
  }

  // ── Per-field validation ──────────────────────────────────────────────────
  flatNoError = computed((): string | null => {
    if (!this.isTouched('flatNo')) return null;
    const v = this.flatNo().trim();
    if (!v) return 'Flat/House No is required';
    if (v.length < 1) return 'Please enter your flat or house number';
    return null;
  });

  streetError = computed((): string | null => {
    if (!this.isTouched('street')) return null;
    const v = this.street().trim();
    if (!v) return 'Street name is required';
    if (v.length < 3) return 'Street name is too short';
    return null;
  });

  areaError = computed((): string | null => {
    if (!this.isTouched('area')) return null;
    const v = this.area().trim();
    if (!v) return 'Area / Locality is required';
    if (v.length < 3) return 'Area name is too short';
    return null;
  });

  cityError = computed((): string | null => {
    if (!this.isTouched('city')) return null;
    const v = this.city().trim();
    if (!v) return 'City is required';
    if (v.length < 2) return 'City name is too short';
    if (!/^[a-zA-Z\s\-\.]+$/.test(v)) return 'City should only contain letters';
    return null;
  });

  stateError = computed((): string | null => {
    if (!this.isTouched('state')) return null;
    const v = this.state().trim();
    if (!v) return 'State is required';
    const isValid = INDIAN_STATES.some(
      s => s.toLowerCase() === v.toLowerCase()
    );
    if (!isValid) return 'Please enter a valid Indian state or UT';
    return null;
  });

  pinError = computed((): string | null => {
    if (!this.isTouched('pinCode')) return null;
    const v = this.pinCode().trim();
    if (!v) return 'PIN code is required';
    if (!/^[1-9][0-9]{5}$/.test(v)) return 'Enter a valid 6-digit Indian PIN code';
    return null;
  });

  // State suggestions as user types
  stateSuggestions = computed((): string[] => {
    const v = this.state().trim().toLowerCase();
    if (v.length < 2) return [];
    return INDIAN_STATES.filter(s => s.toLowerCase().startsWith(v)).slice(0, 5);
  });

  // Compose the final address string — sent to backend
  composedAddress = computed((): string => {
    const parts = [
      this.flatNo().trim(),
      this.street().trim(),
      this.area().trim(),
      this.city().trim(),
      this.state().trim(),
      this.pinCode().trim()
    ].filter(Boolean);
    return parts.join(', ');
  });

  // All fields valid
  addressFormValid = computed((): boolean => {
    return (
      this.flatNo().trim().length >= 1 &&
      this.street().trim().length >= 3 &&
      this.area().trim().length >= 3 &&
      this.city().trim().length >= 2 &&
      /^[a-zA-Z\s\-\.]+$/.test(this.city().trim()) &&
      INDIAN_STATES.some(s => s.toLowerCase() === this.state().trim().toLowerCase()) &&
      /^[1-9][0-9]{5}$/.test(this.pinCode().trim())
    );
  });

  canPlaceOrder = computed((): boolean => {
    if (this.manualEntry()) return this.addressFormValid();
    return this.shippingAddress.trim().length >= 10;
  });

  // ── Mock payment modal ────────────────────────────────────────────────────
  showModal       = signal(false);
  modalProcessing = signal(false);
  cancelled       = signal(false);
  modalError      = signal<string | null>(null);
  payMethod       = signal<PayMethod>('card');
  countdown       = signal(3);
  private countdownInterval: any = null;

  mockCard   = '';
  mockExpiry = '';
  mockCvv    = '';
  mockName   = '';
  mockUpi    = '';
  mockBank   = '';

  readonly bankOpts = [
    { value: 'sbi',   label: 'State Bank of India'  },
    { value: 'hdfc',  label: 'HDFC Bank'             },
    { value: 'icici', label: 'ICICI Bank'            },
    { value: 'axis',  label: 'Axis Bank'             },
    { value: 'kotak', label: 'Kotak Mahindra Bank'   },
  ];

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }
    this.loadSavedAddresses();
    this.cartSvc.getCart().subscribe({
      next: (c)  => { this.cart.set(c); this.loading.set(false); },
      error: ()  => { this.error.set('Could not load cart.'); this.loading.set(false); }
    });
  }

  private loadSavedAddresses(): void {
    const stored = localStorage.getItem(`runway_addresses_${this.auth.userId() ?? 'guest'}`);
    if (stored) {
      const addresses: SavedAddress[] = JSON.parse(stored);
      this.savedAddresses.set(addresses);
      const def = addresses.find(a => a.isDefault) ?? addresses[0];
      if (def) {
        this.selectedAddressId.set(def.id);
        this.shippingAddress = def.address;
        this.manualEntry.set(false);
      }
    } else {
      this.orderSvc.getMyOrders(0, 5).subscribe({
        next: (res) => {
          const seen = new Set<string>();
          const fromOrders: SavedAddress[] = [];
          res.content.forEach((order, idx) => {
            if (order.shippingAddress && !seen.has(order.shippingAddress)
                && fromOrders.length < 3) {
              seen.add(order.shippingAddress);
              fromOrders.push({
                id: `order_${order.id}`,
                label: idx === 0 ? 'Home' : `Address ${idx + 1}`,
                address: order.shippingAddress,
                isDefault: idx === 0
              });
            }
          });
          if (fromOrders.length > 0) {
            this.savedAddresses.set(fromOrders);
            localStorage.setItem(`runway_addresses_${this.auth.userId() ?? 'guest'}`, JSON.stringify(fromOrders));
            this.selectedAddressId.set(fromOrders[0].id);
            this.shippingAddress = fromOrders[0].address;
            this.manualEntry.set(false);
          } else {
            this.manualEntry.set(true);
            this.selectedAddressId.set('new');
          }
        },
        error: () => { this.manualEntry.set(true); this.selectedAddressId.set('new'); }
      });
    }
  }

  onAddressSelect(id: string): void {
    this.selectedAddressId.set(id);
    this.touched.set(new Set()); // reset touched on selection change
    if (id === 'new') {
      this.shippingAddress = '';
      this.manualEntry.set(true);
    } else {
      const addr = this.savedAddresses().find(a => a.id === id);
      if (addr) {
        this.shippingAddress = addr.address;
        this.manualEntry.set(false);
      }
    }
  }

  showSuggestions = signal(true);

  
  selectStateSuggestion(state: string): void {
    this.state.set(state);
    this.touch('state');
    this.showSuggestions.set(false);
  }

  // ── Step 1: Place order ───────────────────────────────────────────────────
  placeOrder(): void {
    // Touch all fields to show all errors at once
    if (this.manualEntry()) {
      this.touched.set(new Set(['flatNo','street','area','city','state','pinCode']));
      if (!this.addressFormValid()) {
        this.error.set('Please fill in all address fields correctly.');
        return;
      }
      this.shippingAddress = this.composedAddress();
      // Save the new address to user-scoped localStorage
      // so it appears in the Account addresses page immediately
      this.saveNewAddressToStorage(this.shippingAddress);
    }

    if (this.shippingAddress.trim().length < 10) {
      this.error.set('Please select or enter a valid shipping address.');
      return;
    }

    this.processing.set(true);
    this.error.set(null);

    this.orderSvc.placeOrder({
      shippingAddress: this.shippingAddress.trim(),
      notes: this.notes.trim() || undefined
    } as CheckoutRequest).subscribe({
      next: (order) => {
        this.orderId.set(order.id);
        this.orderNumber.set(order.orderNumber);
        this.currency.set(order.currency);

        this.paymentSvc.createIntent(order.id, order.totalAmount).subscribe({
          next: (intent) => {
            this.paymentIntent.set(intent);
            this.step.set('payment');
            this.processing.set(false);
          },
          error: (err) => {
            this.error.set(err?.error?.message ?? `Payment setup failed (${err.status})`);
            this.processing.set(false);
          }
        });
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Failed to place order.');
        this.processing.set(false);
      }
    });
  }

  // ── Modal controls ────────────────────────────────────────────────────────
  openPaymentModal(): void {
    this.modalError.set(null);
    this.cancelled.set(false);
    this.showModal.set(true);
  }

  closeModal(): void {
    if (!this.modalProcessing()) this.showModal.set(false);
  }

  formatCard(event: Event): void {
    const input  = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 16);
    this.mockCard = digits.replace(/(.{4})/g, '$1 ').trim();
  }

  formatPin(event: Event): void {
    const input  = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 6);
    this.pinCode.set(digits);
    input.value = digits;
  }

  submitMockPayment(): void {
    this.modalError.set(null);

    if (this.payMethod() === 'card') {
      if (this.mockCard.replace(/\s/g, '').length < 12) {
        this.modalError.set('Please enter a valid card number.'); return;
      }
      if (!this.mockExpiry) {
        this.modalError.set('Please enter expiry date.'); return;
      }
      if (this.mockCvv.length < 3) {
        this.modalError.set('Please enter a 3-digit CVV.'); return;
      }
    }

    if (this.payMethod() === 'upi' && !this.mockUpi.includes('@')) {
      this.modalError.set('Please enter a valid UPI ID (e.g. name@upi).'); return;
    }

    if (this.payMethod() === 'netbanking' && !this.mockBank) {
      this.modalError.set('Please select a bank.'); return;
    }

    this.showModal.set(false);
    this.cancelled.set(false);
    this.countdown.set(3);
    this.modalProcessing.set(true);

    this.countdownInterval = setInterval(() => {
      const current = this.countdown();
      if (current <= 1) {
        clearInterval(this.countdownInterval);
        this.countdownInterval = null;
        if (!this.cancelled()) this.confirmPayment();
      } else {
        this.countdown.set(current - 1);
      }
    }, 1000);
  }

  cancelProcessing(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
      this.countdownInterval = null;
    }
    this.cancelled.set(true);
    this.modalProcessing.set(false);

    const oid = this.orderId();
    if (oid) {
      this.paymentSvc.updateStatus(oid, 'FAILED').subscribe({
        next: () => console.log('[Checkout] Payment cancelled — marked FAILED'),
        error: () => console.warn('[Checkout] Could not mark as FAILED')
      });
    }
  }

  private confirmPayment(): void {
    const oid = this.orderId();
    if (!oid) return;

    this.paymentSvc.confirmPayment(oid).subscribe({
      next: (intent) => {
        this.paymentIntent.set(intent);
        this.modalProcessing.set(false);
        this.step.set('confirmed');
      },
      error: (err) => {
        console.error('[Checkout] confirmPayment failed:', err?.error?.message);
        this.modalProcessing.set(false);
        this.step.set('failed');
      }
    });
  }

  // ── Save new manually-entered address to user-scoped localStorage ──────────
  // Called after address form validation passes in placeOrder()
  // Uses the same key format as the account page: runway_addresses_<userId>
  // Max 3 addresses — if already at max, replaces the oldest non-default one
  private saveNewAddressToStorage(address: string): void {
    const userId  = this.auth.userId() ?? 'guest';
    const key     = `runway_addresses_${userId}`;
    const stored  = localStorage.getItem(key);
    const current: any[] = stored ? JSON.parse(stored) : [];

    // Don't add duplicate
    if (current.some((a: any) => a.address === address)) return;

    const newEntry = {
      id:        `addr_${Date.now()}`,
      label:     `Address ${current.length + 1}`,
      address,
      isDefault: current.length === 0   // default if first one
    };

    let updated = [...current];

    if (updated.length >= 3) {
      // Remove oldest non-default to make room
      const nonDefaultIdx = updated.findIndex((a: any) => !a.isDefault);
      if (nonDefaultIdx !== -1) {
        updated.splice(nonDefaultIdx, 1);
      } else {
        return; // all 3 are default protected — don't overwrite
      }
    }

    updated.push(newEntry);
    localStorage.setItem(key, JSON.stringify(updated));
  }

  goToOrders():  void { this.router.navigate(['/orders']); }
  goToAccount(): void { this.router.navigate(['/account']); }
  retryCart():   void { this.router.navigate(['/cart']); }
}