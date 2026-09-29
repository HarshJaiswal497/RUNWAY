import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DecimalPipe, TitleCasePipe } from '@angular/common';
import { CatalogService } from '../../core/services/catalog.service';
import { RatingService, Rating } from '../../core/services/rating.service';
import { ProductResponse, MediaResponse } from '../../core/models/product.model';
import { AuthService } from '../../core/services/auth.service';
import { ReviewsComponent } from '../reviews/reviews.component';
import { ReviewsService } from '../../core/services/reviews.service';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
    selector: 'app-product-detail',
    standalone: true,
    imports: [DecimalPipe, TitleCasePipe, ReviewsComponent],
    templateUrl: './product-detail.component.html',
    styleUrl: './product-detail.component.css'
})
export class ProductDetailComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private catalog = inject(CatalogService);
    private ratingService = inject(RatingService);
    private reviewsService = inject(ReviewsService);
    private toast = inject(ToastService);

    // seller info
    private authService = inject(AuthService);
    private cartService = inject(CartService);

    sellerName = signal<string>('');

    product = signal<ProductResponse | null>(null);
    relatedProducts = signal<ProductResponse[]>([]);
    ratings = signal<Rating[]>([]);
    reviews = signal<any[]>([]);
    loadingReviews = signal(true);

    isWishlisted = signal(false);
    wishlistLoading = signal(false);


    // ── Loading states ────────────────────────────────
    loadingProduct = signal(true);
    loadingRelated = signal(true);
    loadingRatings = signal(true);
    error = signal<string | null>(null);

    selectedImageIndex = signal(0);
    selectedSize = signal<string | null>(null);
    selectedColor = signal<string | null>(null);
    openAccordion = signal<string | null>('description');
    addedToBag = signal(false);
    addingToCart = signal(false);
    cartError = signal<string | null>(null);

    selectedImage = computed(() => {
        const media = this.product()?.media ?? [];
        return media[this.selectedImageIndex()]?.fullUrl ?? '/placeholder.jpg';
    });

    lensImageUrl = computed(() => `url("${encodeURI(this.selectedImage())}")`);

    sizes = computed(() => {
        const s = this.product()?.attributes?.['size'];
        if (!s) return [];
        return Array.isArray(s) ? s : [s];
    });

    colors = computed(() => {
        const c = this.product()?.attributes?.['color'];
        if (!c) return [];
        return Array.isArray(c) ? c : [c];
    });

    averageRating(): number {
        const list = this.ratings();
        if (!list || list.length === 0) return 0;
        const sum = list.reduce((acc, r) => acc + (r.rating), 0);
        return sum / list.length;
    }

    ratingCount(): number {
        const list = this.ratings();
        return list ? list.length : 0;
    }

    starArray(): string[] {
        const avg = this.averageRating();
        return [1, 2, 3, 4, 5].map(n => {
            if (n <= Math.floor(avg)) return 'full';
            if (n - avg < 1 && n - avg > 0) return 'half';
            return 'empty';
        });
    };

    //   zoom lens
    // ── Zoom lens state ───────────────────────────────
    showLens = signal(false);
    lensX = signal(0);
    lensY = signal(0);
    lensBackgroundPosition = signal('0px 0px');
    lensBackgroundSize = signal('0px 0px');
    private readonly LENS_SIZE = 120;
    private readonly ZOOM_FACTOR = 2.5;

    onMouseEnter(): void { this.showLens.set(true); }
    onMouseLeave(): void { this.showLens.set(false); }

    onMouseMove(event: MouseEvent): void {
        const wrap = event.currentTarget as HTMLElement;
        const rect = wrap.getBoundingClientRect();
        const half = this.LENS_SIZE / 2;
        const x = event.clientX - rect.left;
        const y = event.clientY - rect.top;
        const lensLeft = Math.max(half, Math.min(x, rect.width - half));
        const lensTop = Math.max(half, Math.min(y, rect.height - half));
        this.lensX.set(lensLeft - half);
        this.lensY.set(lensTop - half);
        const bgW = rect.width * this.ZOOM_FACTOR;
        const bgH = rect.height * this.ZOOM_FACTOR;
        this.lensBackgroundSize.set(`${bgW}px ${bgH}px`);
        const bgX = (lensLeft * this.ZOOM_FACTOR) - half;
        const bgY = (lensTop * this.ZOOM_FACTOR) - half;
        this.lensBackgroundPosition.set(`-${bgX}px -${bgY}px`);
    }

    private pendingFragment: string | null = null;

    ngOnInit(): void {
        this.route.fragment.subscribe(f => { this.pendingFragment = f ?? null; });
        this.route.params.subscribe(params => {
            const id = Number(params['id']);
            this.resetState();
            if (!this.pendingFragment) window.scrollTo({ top: 0, behavior: 'instant' });
            this.loadProduct(id);
        });
    }

    private resetState(): void {
        this.product.set(null);
        this.selectedImageIndex.set(0);
        this.selectedSize.set(null);
        this.selectedColor.set(null);
        this.addedToBag.set(false);
        this.addingToCart.set(false);
        this.cartError.set(null);
        this.loadingProduct.set(true);
        this.loadingRelated.set(true);
        this.loadingRatings.set(true);
        this.error.set(null);
    }

    private loadProduct(id: number): void {
        this.catalog.getProductById(id).subscribe({
            next: (product) => {
                this.product.set(product);
                this.loadingProduct.set(false);
                // Check wishlist status after product loads
                if (this.authService.isLoggedIn()) {
                    this.catalog.checkWishlisted(product.id).subscribe({
                        next: (status) => this.isWishlisted.set(status),
                        error: () => { }
                    });
                }
                // Fetch seller name
                this.loadSellerName(product.sellerId);
                const colors = this.colors();
                if (colors.length === 1) this.selectedColor.set(colors[0]);
                this.loadRelated(product.categoryId, id);
                this.loadReviews(id);
                this.loadRatings(id);
                if (this.pendingFragment) {
                    const frag = this.pendingFragment;
                    this.pendingFragment = null;
                    setTimeout(() => document.getElementById(frag)?.scrollIntoView({ behavior: 'smooth' }), 150);
                }
            },
            error: () => { this.error.set('Product not found.'); this.loadingProduct.set(false); }
        });
    }

    private loadSellerName(sellerId: number): void {
        this.authService.getUserDetails(sellerId).subscribe({
            next: (res) => this.sellerName.set(res.user.name),
            error: () => this.sellerName.set('RunWay Curated')
        });
    }

    private loadRelated(categoryId: number, excludeId: number): void {
        this.catalog.getProductsByCategory(categoryId).subscribe({
            next: (products) => {
                this.relatedProducts.set(products.filter(p => p.id !== excludeId).slice(0, 4));
                this.loadingRelated.set(false);
            },
            error: () => this.loadingRelated.set(false)
        });
    }

    private loadRatings(productId: number): void {
        this.ratingService.getProductRatings(productId).subscribe({
            next: (data) => {
                // console.log('Ratings API:', data);
                this.ratings.set(data);
                this.loadingRatings.set(false);
            },
            error: () => this.loadingRatings.set(false)
        });
    }

    private loadReviews(productId: number): void {
        this.reviewsService.getReviews(productId).subscribe({
            next: (data) => {
                this.reviews.set(data);
                this.loadingReviews.set(false);
            },
            error: (err) => {
                this.loadingReviews.set(false)
            }
        });
    }


    // ── Actions ───────────────────────────────────────
    selectImage(index: number): void { this.selectedImageIndex.set(index); }
    selectSize(size: string): void { this.selectedSize.set(this.selectedSize() === size ? null : size); }
    selectColor(color: string): void { this.selectedColor.set(color); }
    toggleAccordion(key: string): void { this.openAccordion.set(this.openAccordion() === key ? null : key); }

    addToBag(): void {
        if (!this.selectedSize() && this.sizes().length > 0) return;
        const p = this.product();
        if (!p) return;

        if (!this.authService.isLoggedIn()) { this.router.navigate(['/login']); return; }

        this.cartError.set(null);
        this.addingToCart.set(true);

        // No userId — cartService.addItem uses JWT
        this.cartService.addItem({
            productId: p.id,
            productTitle: p.title,
            unitPrice: p.price,
            quantity: 1
        }).subscribe({
            next: () => {
                this.addingToCart.set(false);
                this.addedToBag.set(true);
                setTimeout(() => { this.addedToBag.set(false); this.router.navigate(['/cart']); }, 1000);
            },
            error: (err) => {
                this.addingToCart.set(false);
                this.cartError.set(err?.error?.message ?? 'Could not add to bag. Please try again.');
            }
        });
    }

    goToProduct(id: number): void { this.router.navigate(['/product', id]); window.scrollTo({ top: 0, behavior: 'instant' }); }
    goBack(): void { window.history.back(); }

    getPrimaryImage(media: MediaResponse[]): string {
        if (!media || media.length === 0) return '/placeholder.jpg';
        const primary = media.find(m => m.isPrimary);
        return primary ? primary.fullUrl : media[0].fullUrl;
    }

    getStarPercent(rating: number): number {
        if (this.ratingCount() === 0) return 0;
        return (this.ratings().filter(r => Math.round(r.rating) === rating).length
            / this.ratingCount()) * 100;
    }

    toggleWishlist(): void {
        if (!this.authService.isLoggedIn()) {
            this.router.navigate(['/login']);
            return;
        }

        const product = this.product();
        if (!product) return;

        this.wishlistLoading.set(true);

        if (this.isWishlisted()) {
            this.catalog.removeFromWishlist(product.id).subscribe({
                next: () => {
                    this.isWishlisted.set(false);
                    this.wishlistLoading.set(false);
                    this.toast.show('Removed from Wishlist', product.title);
                },
                error: () => this.wishlistLoading.set(false)
            });
        } else {
            this.catalog.addToWishlist(product.id).subscribe({
                next: () => {
                    this.isWishlisted.set(true);
                    this.wishlistLoading.set(false);
                    this.toast.show('Added to Wishlist', product.title);
                },
                error: () => this.wishlistLoading.set(false)
            });
        }
    }

    readonly Object = Object;
    objectEntries = (obj: Record<string, any>) => Object.entries(obj);
    isArray = (val: any) => Array.isArray(val);
}