import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { CatalogService } from '../../core/services/catalog.service';
import { ToastService } from '../../core/services/toast.service';
import { ProductResponse, MediaResponse } from '../../core/models/product.model';

@Component({
    selector: 'app-wishlist',
    standalone: true,
    imports: [DecimalPipe],
    templateUrl: './wishlist.component.html',
    styleUrl: './wishlist.component.css'
})
export class WishlistComponent implements OnInit {
    private catalog = inject(CatalogService);
    router = inject(Router);
    private toast = inject(ToastService);

    products = signal<ProductResponse[]>([]);
    loading = signal(true);
    removing = signal<number | null>(null);

    ngOnInit(): void {
        window.scrollTo({ top: 0, behavior: 'instant' });
        this.loadWishlist();
    }

    private loadWishlist(): void {
        this.loading.set(true);
        this.catalog.getWishlist().subscribe({
            next: (products) => {
                this.products.set(products);
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
    }

    removeItem(productId: number, title: string): void {
        this.removing.set(productId);
        this.catalog.removeFromWishlist(productId).subscribe({
            next: () => {
                this.products.update(ps => ps.filter(p => p.id !== productId));
                this.removing.set(null);
                this.toast.show('Removed from Wishlist', title);
            },
            error: () => this.removing.set(null)
        });
    }

    goToProduct(id: number): void {
        this.router.navigate(['/product', id]);
    }

    getPrimaryImage(media: MediaResponse[]): string {
        if (!media?.length) return '/placeholder.jpg';
        const primary = media.find(m => m.isPrimary);
        return encodeURI(primary ? primary.fullUrl : media[0].fullUrl);
    }
}