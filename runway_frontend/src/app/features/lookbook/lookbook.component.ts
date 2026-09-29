import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { CatalogService } from '../../core/services/catalog.service';
import { ProductResponse, MediaResponse } from '../../core/models/product.model';

interface LookProduct {
  sku: string;
  label: string;
  product?: ProductResponse;
}

interface Look {
  id: string;
  season: string;
  title: string;
  subtitle: string;
  copy: string;
  keywords: string[];
  products: LookProduct[];
  activeImageIndex: number;
}

@Component({
  selector: 'app-lookbook',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './lookbook.component.html',
  styleUrl: './lookbook.component.css'
})
export class LookbookComponent implements OnInit, OnDestroy {
  private catalog = inject(CatalogService);
  private router  = inject(Router);

  loading   = signal(true);
  private timers: any[] = [];

  readonly looks: Look[] = [
    {
      id: '01',
      season: "A/W '26 — Look 01",
      title: 'The Evening',
      subtitle: 'Dressed for the room you command.',
      copy: 'Floor-length architecture in matte black crepe. Gold that whispers where others shout. This is dressing with conviction — for those who arrive without announcement.',
      keywords: ['POWER', 'PRECISION', 'POISE'],
      products: [
        { sku: 'APP-001', label: 'Black Stair Gown' },
        { sku: 'APP-002', label: 'Gold Black Hallway Gown' },
        { sku: 'ACC-006', label: 'Monochrome Dress Watch' },
      ],
      activeImageIndex: 0
    },
    {
      id: '02',
      season: "A/W '26 — Look 02",
      title: 'The Authority',
      subtitle: 'Tailored for those who lead.',
      copy: 'A suit is not clothing — it is a statement of intent. Pure wool twill, cut to command. Chrome oxfords that mark every step with purpose. The essential vocabulary of power, spoken fluently.',
      keywords: ['STRUCTURE', 'SILENCE', 'STRENGTH'],
      products: [
        { sku: 'APP-007', label: 'Black Suit' },
        { sku: 'APP-012', label: 'Black Gold Accent Suit' },
        { sku: 'FTW-001', label: 'Chrome Oxford' },
      ],
      activeImageIndex: 0
    },
    {
      id: '03',
      season: "A/W '26 — Look 03",
      title: 'The Feminine',
      subtitle: 'Refined. Resolute. Radiant.',
      copy: 'Liquid silver satin that moves with every intention. A sculptural heel in deep crimson that needs no introduction. Gold at the ear — handcrafted, unhurried. This is femininity on its own terms.',
      keywords: ['GRACE', 'EDGE', 'INTENTION'],
      products: [
        { sku: 'APP-003', label: 'Silver Hallway Gown' },
        { sku: 'FTW-007', label: 'Crimson Obsidian Stiletto' },
        { sku: 'ACC-004', label: 'Sculptural Drop Earrings' },
      ],
      activeImageIndex: 0
    },
    {
      id: '04',
      season: "A/W '26 — Look 04",
      title: 'The Statement',
      subtitle: 'For those who enter a room first.',
      copy: 'Full crimson in brushed velvet — uncompromisingly bold. Liquid gold at the heel, catching every light in every room. Dark frames that say everything before a word is spoken. This is not fashion. This is arrival.',
      keywords: ['BOLDNESS', 'THEATRE', 'DEFIANCE'],
      products: [
        { sku: 'APP-010', label: 'Red Suit' },
        { sku: 'FTW-009', label: 'Liquid Gold Eclipse Heel' },
        { sku: 'ACC-005', label: 'Architectural Sunglasses' },
      ],
      activeImageIndex: 0
    },
  ];

  ngOnInit(): void {
    window.scrollTo({ top: 0, behavior: 'instant' });
    this.catalog.getAllProducts().subscribe({
      next: (products) => {
        this.looks.forEach(look => {
          look.products.forEach(lp => {
            lp.product = products.find(p => p.sku === lp.sku);
          });
        });
        this.loading.set(false);
        // Start auto-cycling after products loaded
        setTimeout(() => this.startImageCycles(), 300);
      },
      error: () => this.loading.set(false)
    });
  }

  ngOnDestroy(): void {
    this.timers.forEach(t => clearInterval(t));
  }

  private startImageCycles(): void {
    this.looks.forEach((look, i) => {
      const validProducts = look.products.filter(p => p.product?.media?.length);
      if (validProducts.length <= 1) return;
      // Stagger start time per look so they don't all flip together
      const timer = setInterval(() => {
        look.activeImageIndex =
          (look.activeImageIndex + 1) % validProducts.length;
      }, 4000 + i * 500);
      this.timers.push(timer);
    });
  }

  scrollToLook(id: string): void {
    const el = document.getElementById('look-' + id);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  goToProduct(productId: number | undefined): void {
    if (!productId) return;
    this.router.navigate(['/product', productId]);
  }

  getActiveImage(look: Look): string {
    const validProducts = look.products.filter(p => p.product?.media?.length);
    if (!validProducts.length) return '';
    const product = validProducts[look.activeImageIndex % validProducts.length].product!;
    const primary = product.media.find((m: MediaResponse) => m.isPrimary);
    const url = primary ? primary.fullUrl : product.media[0].fullUrl;
    return encodeURI(url);
  }

  getPrimaryImage(media: MediaResponse[]): string {
    if (!media?.length) return '';
    const primary = media.find(m => m.isPrimary);
    return encodeURI(primary ? primary.fullUrl : media[0].fullUrl);
  }

  getImageDots(look: Look): number[] {
    const count = look.products.filter(p => p.product?.media?.length).length;
    return Array(count).fill(0);
  }
}