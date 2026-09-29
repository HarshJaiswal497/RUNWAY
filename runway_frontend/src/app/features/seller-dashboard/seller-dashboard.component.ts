import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';
import { ProductResponse, CategoryResponse, MediaResponse } from '../../core/models/product.model';
import { CatalogService } from '../../core/services/catalog.service';
import { RwSelectComponent } from '../../shared/rw-select/rw-select.component';

type Section = 'overview' | 'products' | 'inventory' | 'account';

interface InventoryResponse {
    productId: number;
    currentQuantity: number;
    status: string;
    message: string;
}

@Component({
    selector: 'app-seller-dashboard',
    standalone: true,
    imports: [DecimalPipe, FormsModule, RwSelectComponent],
    templateUrl: './seller-dashboard.component.html',
    styleUrl: './seller-dashboard.component.css'
})
export class SellerDashboardComponent implements OnInit {
    private authService = inject(AuthService);
    private catalog = inject(CatalogService);
    private api = inject(ApiService);
    private router = inject(Router);

    // ── UI state ──────────────────────────────────────
    activeSection = signal<Section>('overview');
    sidebarOpen = signal(true);

    // ── Data ──────────────────────────────────────────
    myProducts    = signal<ProductResponse[]>([]);
    allCategories = signal<CategoryResponse[]>([]);
    userDetails   = signal<any>(null);
    inventoryMap  = signal<Record<number, InventoryResponse>>({});

    // ── rw-select option lists ─────────────────────────
    get childCategoryOpts() {
        return this.allCategories()
            .filter(c => c.parentId && c.parentId !== 0)
            .map(c => ({ value: c.id, label: c.name }));
    }
    readonly genderOpts = [
        { value: 'Male',   label: 'Male'   },
        { value: 'Female', label: 'Female' },
        { value: 'Unisex', label: 'Unisex' },
    ];

    // ── Loading ───────────────────────────────────────
    loadingProducts = signal(true);
    loadingUser = signal(true);

    // ── Modal state ───────────────────────────────────
    showCreateModal = signal(false);
    showEditModal = signal(false);
    showDeleteModal = signal(false);
    showInvModal = signal(false);
    selectedProduct = signal<ProductResponse | null>(null);
    modalLoading = signal(false);
    modalError = signal<string | null>(null);
    modalSuccess = signal<string | null>(null);

    // ── Create product form ───────────────────────────
    createForm = {
        title: '',
        description: '',
        price: 0,
        currency: 'INR',
        categoryId: 0,
        sku: '',
        gender: '',
        material: '',
        color: '',
        occasion: '',
        size: '',
        stock: 0,
        files: [] as File[]
    };

    // ── Edit form ─────────────────────────────────────
    editForm = {
        title: '',
        description: '',
        price: 0,
        categoryId: 0,
        sku: '',
        currency: 'INR',
        initialQuantity: 0,
        attributes: {} as Record<string, any>
    };

    // ── Inventory form ────────────────────────────────
    invForm = { quantity: 0, operation: 'SET' };

    // ── Account ───────────────────────────────────────
    showDeleteAccountModal = signal(false);

    // ── Computed ──────────────────────────────────────
    sellerId = computed(() => this.authService.userId());

    lowStockProducts = computed(() =>
        this.myProducts().filter(p => {
            const inv = this.inventoryMap()[p.id];
            return inv && inv.currentQuantity <= 5;
        })
    );

    // Preview URLs cache
    previewUrls: string[] = [];
    // Edit modal image state
    editFiles: File[] = [];
    editPreviewUrls: string[] = [];
    deletingImageId = signal<number | null>(null);


    ngOnInit(): void {
        this.loadUserDetails();
        this.loadMyProducts();
        this.catalog.getAllCategories().subscribe({
            next: (cats) => this.allCategories.set(cats),
            error: () => { }
        });
    }

    // ── Data loading ──────────────────────────────────
    private loadUserDetails(): void {
        const userId = this.sellerId();
        if (!userId) return;
        this.authService.getUserDetails(userId).subscribe({
            next: (res) => {
                this.userDetails.set(res.user);
                this.loadingUser.set(false);
            },
            error: () => this.loadingUser.set(false)
        });
    }

    private loadMyProducts(): void {
        const sellerId = this.sellerId();
        if (!sellerId) return;
        this.loadingProducts.set(true);
        this.catalog.getProductsBySeller(sellerId).subscribe({
            next: (products) => {
                this.myProducts.set([...products].reverse());
                this.loadingProducts.set(false);
                // Load inventory for each product
                products.forEach(p => this.loadInventory(p.id));
            },
            error: () => this.loadingProducts.set(false)
        });
    }

    private loadInventory(productId: number): void {
        this.api.get<any>(`/catalog/inventory/${productId}`).subscribe({
            next: (inv) => {
                // API returns just a number directly
                const quantity = typeof inv === 'number' ? inv : inv?.currentQuantity ?? 0;
                const inventoryObj: InventoryResponse = {
                    productId,
                    currentQuantity: quantity,
                    status: quantity === 0 ? 'OUT_OF_STOCK' : quantity <= 5 ? 'LOW_STOCK' : 'IN_STOCK',
                    message: ''
                };
                this.inventoryMap.update(map => ({ ...map, [productId]: inventoryObj }));
            },
            error: () => { }
        });
    }

    // ── Navigation ────────────────────────────────────
    setSection(section: Section): void {
        this.activeSection.set(section);
    }

    toggleSidebar(): void {
        this.sidebarOpen.update(v => !v);
    }

    logout(): void {
        this.authService.logout();
    }

    // ── Create product ────────────────────────────────
    openCreateModal(): void {
        // Revoke any existing preview URLs
        this.previewUrls.forEach(url => URL.revokeObjectURL(url));
        this.previewUrls = [];
        this.createForm = {
            title: '', description: '', price: 0, currency: 'INR',
            categoryId: 0, sku: '', gender: '', material: '',
            color: '', occasion: '', size: '', stock: 0, files: []
        };
        this.modalError.set(null);
        this.modalSuccess.set(null);
        this.showCreateModal.set(true);
    }

    onFileSelect(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files) {
            const newFiles = Array.from(input.files);
            this.createForm.files = [...this.createForm.files, ...newFiles];
            // Generate preview URLs
            newFiles.forEach(file => {
                const url = URL.createObjectURL(file);
                this.previewUrls.push(url);
            });
        }
    }

    removeFile(index: number): void {
        // Revoke the object URL to free memory
        URL.revokeObjectURL(this.previewUrls[index]);
        this.previewUrls.splice(index, 1);
        this.createForm.files.splice(index, 1);
        // Trigger change detection
        this.createForm.files = [...this.createForm.files];
    }

    getPreviewUrl(file: File): string {
        const index = this.createForm.files.indexOf(file);
        return this.previewUrls[index] ?? '';
    }

    invalidPriceMsg = 'Price cannot be zero or negative';
    invalidQuantityMsg = 'Quantity cannot be negative';

    priceInvalid = signal<null | boolean>(null);
    quantityInvalid = signal<null | boolean>(null);

    checkIfPriceInvalid(price:number):void{
        if(price<1){
            this.priceInvalid.set(true);
        }
        else{
            this.priceInvalid.set(false);
        }
    }

    checkIfQuantityInvalid(quantity:number):void{
        if(quantity<0){
            this.quantityInvalid.set(true);
        }
        else{
            this.quantityInvalid.set(false);
        }
       
    }

    submitCreateProduct(): void {
        if (!this.createForm.title || !this.createForm.price || !this.createForm.categoryId) {
            this.modalError.set('Please fill in all required fields.');
            return;
        }

        this.checkIfPriceInvalid(this.createForm.price);
        this.checkIfQuantityInvalid(this.createForm.stock);

        this.modalLoading.set(true);
        this.modalError.set(null);

        const formData = new FormData();

        // ── Product JSON as 'product' part ──────────────
        const attributes: Record<string, any> = {};
        if (this.createForm.gender) attributes['gender'] = this.createForm.gender;
        if (this.createForm.material) attributes['material'] = this.createForm.material;
        if (this.createForm.color) attributes['color'] = this.createForm.color;
        if (this.createForm.occasion) attributes['occasion'] = this.createForm.occasion;
        if (this.createForm.size) {
            attributes['size'] = this.createForm.size.split(',').map((s: string) => s.trim());
        }

        const productJson = JSON.stringify({
            title: this.createForm.title,
            description: this.createForm.description,
            price: this.createForm.price,
            currency: this.createForm.currency,
            categoryId: this.createForm.categoryId,
            sku: this.createForm.sku,
            initialQuantity: this.createForm.stock,
            attributes
        });

        // Must be sent as 'product' part — backend uses @RequestPart("product")
        formData.append('product', new Blob([productJson], { type: 'application/json' }));

        // ── Images as 'images' part ───────────────────────
        this.createForm.files.forEach(file => {
            formData.append('images', file);
        });

        this.api.postFormData<ProductResponse>('/catalog/products', formData).subscribe({
            next: () => {
                this.modalLoading.set(false);
                this.modalSuccess.set('Product created successfully!');
                this.loadMyProducts();
                setTimeout(() => {
                    this.showCreateModal.set(false);
                    // Clean up preview URLs
                    this.previewUrls.forEach(url => URL.revokeObjectURL(url));
                    this.previewUrls = [];
                }, 1500);
            },
            error: (err) => {
                this.modalLoading.set(false);
                this.modalError.set('Failed to create product. Please try again.');
                console.error('Create product error:', err);
            }
        });
    }


    // ── Edit product ──────────────────────────────────
    onEditFileSelect(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files) {
            const newFiles = Array.from(input.files);
            this.editFiles = [...this.editFiles, ...newFiles];
            newFiles.forEach(file => {
                this.editPreviewUrls.push(URL.createObjectURL(file));
            });
        }
    }

    removeEditFile(index: number): void {
        URL.revokeObjectURL(this.editPreviewUrls[index]);
        this.editPreviewUrls.splice(index, 1);
        this.editFiles.splice(index, 1);
        this.editFiles = [...this.editFiles];
    }

    deleteProductImage(mediaId: number): void {
        this.deletingImageId.set(mediaId);
        this.api.delete(`/catalog/products/images/${mediaId}`).subscribe({
            next: () => {
                this.deletingImageId.set(null);
                // Remove from selected product's media locally
                const product = this.selectedProduct();
                if (product) {
                    const updatedMedia = product.media.filter(m => m.id !== mediaId);
                    this.selectedProduct.set({ ...product, media: updatedMedia });
                }
                // Reload products in background
                this.loadMyProducts();
            },
            error: () => {
                this.deletingImageId.set(null);
                this.modalError.set('Failed to delete image.');
            }
        });
    }

    openEditModal(product: ProductResponse): void {
        this.selectedProduct.set(product);
        this.editFiles = [];
        this.editPreviewUrls.forEach(url => URL.revokeObjectURL(url));
        this.editPreviewUrls = [];
        this.editForm = {
            title: product.title,
            description: product.description,
            price: product.price,
            categoryId: product.categoryId,
            sku: product.sku,
            currency: product.currency ?? 'INR',
            initialQuantity: 0,
            attributes: product.attributes ?? {}
        };
        this.modalError.set(null);
        this.modalSuccess.set(null);
        this.showEditModal.set(true);
    }

    submitEditProduct(): void {
        const product = this.selectedProduct();
        if (!product) return;

        this.modalLoading.set(true);
        this.modalError.set(null);

        const payload = {
            title: this.editForm.title,
            description: this.editForm.description,
            price: this.editForm.price,
            categoryId: this.editForm.categoryId,
            sku: this.editForm.sku,
            currency: this.editForm.currency,
            initialQuantity: this.editForm.initialQuantity,
            attributes: this.editForm.attributes
        };

        this.api.put<ProductResponse>(`/catalog/products/${product.id}`, payload).subscribe({
            next: () => {
                // If new images selected, upload them too
                if (this.editFiles.length > 0) {
                    const formData = new FormData();
                    this.editFiles.forEach(file => formData.append('images', file));

                    this.api.postFormData<void>(
                        `/catalog/products/${product.id}/images`, formData
                    ).subscribe({
                        next: () => {
                            this.modalLoading.set(false);
                            this.modalSuccess.set('Product updated successfully!');
                            this.loadMyProducts();
                            setTimeout(() => {
                                this.showEditModal.set(false);
                                this.editFiles = [];
                                this.editPreviewUrls.forEach(url => URL.revokeObjectURL(url));
                                this.editPreviewUrls = [];
                            }, 1500);
                        },
                        error: () => {
                            this.modalLoading.set(false);
                            this.modalError.set('Product updated but failed to upload new images.');
                        }
                    });
                } else {
                    this.modalLoading.set(false);
                    this.modalSuccess.set('Product updated successfully!');
                    this.loadMyProducts();
                    setTimeout(() => this.showEditModal.set(false), 1500);
                }
            },
            error: () => {
                this.modalLoading.set(false);
                this.modalError.set('Failed to update product.');
            }
        });
    }

    // ── Delete product ────────────────────────────────
    openDeleteModal(product: ProductResponse): void {
        this.selectedProduct.set(product);
        this.modalError.set(null);
        this.showDeleteModal.set(true);
    }

    confirmDelete(): void {
        const product = this.selectedProduct();
        if (!product) return;

        this.modalLoading.set(true);
        this.api.delete(`/catalog/products/${product.id}`).subscribe({
            next: () => {
                this.modalLoading.set(false);
                this.showDeleteModal.set(false);
                this.loadMyProducts();
            },
            error: () => {
                this.modalLoading.set(false);
                this.modalError.set('Failed to delete product.');
            }
        });
    }

    // ── Inventory ─────────────────────────────────────
    openInvModal(product: ProductResponse): void {
        this.selectedProduct.set(product);
        this.invForm = { quantity: 0, operation: 'ADD' }; // ← default to ADD
        this.modalError.set(null);
        this.modalSuccess.set(null);
        this.showInvModal.set(true);
    }

    submitInventory(): void {
        const product = this.selectedProduct();
        if (!product) return;

        this.modalLoading.set(true);
        this.modalError.set(null);

        // If SUBTRACT make amount negative
        const amount = this.invForm.operation === 'SUBTRACT'
            ? -Math.abs(this.invForm.quantity)
            : Math.abs(this.invForm.quantity);

        this.api.patch<any>(
            `/catalog/inventory/${product.id}/update?amount=${amount}`,
            {}
        ).subscribe({
            next: (res) => {
                this.modalLoading.set(false);
                this.modalSuccess.set('Inventory updated!');
                const newQty = typeof res === 'number' ? res : res?.currentQuantity ?? 0;
                const inventoryObj: InventoryResponse = {
                    productId: product.id,
                    currentQuantity: newQty,
                    status: newQty === 0 ? 'OUT_OF_STOCK' : newQty <= 5 ? 'LOW_STOCK' : 'IN_STOCK',
                    message: ''
                };
                this.inventoryMap.update(map => ({ ...map, [product.id]: inventoryObj }));
                setTimeout(() => this.showInvModal.set(false), 1500);
            },
            error: () => {
                this.modalLoading.set(false);
                this.modalError.set('Failed to update inventory.');
            }
        });
    }

    // ── Account ───────────────────────────────────────
    confirmDeleteAccount(): void {
        this.api.delete('/auth/deleteaccount').subscribe({
            next: () => {
                this.authService.logout();
            },
            error: () => { }
        });
    }

    // ── Helpers ───────────────────────────────────────
    getPrimaryImage(media: MediaResponse[]): string {
        if (!media?.length) return '/placeholder.jpg';
        const primary = media.find(m => m.isPrimary);
        return primary ? primary.fullUrl : media[0].fullUrl;
    }

    getStockStatus(productId: number): string {
        const inv = this.inventoryMap()[productId];
        if (!inv) return 'unknown';
        if (inv.currentQuantity === 0) return 'out';
        if (inv.currentQuantity <= 5) return 'low';
        return 'in';
    }

    getStockQty(productId: number): number {
        return this.inventoryMap()[productId]?.currentQuantity ?? 0;
    }
}