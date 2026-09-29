import { Component, Input, OnDestroy, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { CreateReviews } from '../create-reviews/create-reviews.component';

@Component({
  selector: 'app-reviews',
  imports: [DecimalPipe, CreateReviews],
  standalone: true,
  templateUrl: './reviews.component.html',
  styleUrl: './reviews.component.css'
})
export class ReviewsComponent implements OnDestroy {

  @Input() ratings: any[]      = [];
  @Input() reviews: any[]      = [];
  @Input() loading             = false;
  @Input() average             = 0;
  @Input() count               = 0;
  @Input() productId: number | null = null;

  readonly starRange      = [1, 2, 3, 4, 5];
  readonly breakdownRange = [5, 4, 3, 2, 1];

  private readonly PAGE_SIZE = 6;
  visibleCount = this.PAGE_SIZE;

  private authService = inject(AuthService);

  // ── Auth helpers ─────────────────────────────────────
  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get userHasReviewed(): boolean {
    const uid = this.authService.userId();
    if (!uid || !this.reviews?.length) return false;
    return this.reviews.some(r => r.userId === uid);
  }

  get canWriteReview(): boolean {
    return !!this.productId && this.isLoggedIn && !this.userHasReviewed;
  }

  onReviewSubmitted(): void {
    // Parent component will reload reviews; just mark as reviewed locally
  }

  // ── Pagination ──────────────────────────────────────
  get visibleReviews(): any[] {
    return (this.reviews ?? []).slice(0, this.visibleCount);
  }

  get hasMore(): boolean {
    return this.visibleCount < (this.reviews?.length ?? 0);
  }

  showMore(): void {
    this.visibleCount = Math.min(
      this.visibleCount + this.PAGE_SIZE,
      this.reviews?.length ?? 0
    );
  }

  // ── Stars (header summary) ───────────────────────────
  getStarState(n: number): 'full' | 'half' | 'empty' {
    const avg = this.average || 0;
    if (n <= Math.floor(avg)) return 'full';
    if (n === Math.ceil(avg) && avg % 1 !== 0) return 'half';
    return 'empty';
  }

  // ── Stars (review cards) ─────────────────────────────
  isCardStarFull(n: number, rating: number): boolean {
    return n <= Math.round(rating || 0);
  }

  cardStarChar(n: number, rating: number): string {
    return this.isCardStarFull(n, rating) ? '★' : '☆';
  }

  // ── Breakdown ────────────────────────────────────────
  getStarCount(star: number): number {
    if (!this.ratings?.length) return 0;
    return this.ratings.filter(r => Math.round(r.rating) === star).length;
  }

  getStarPercent(star: number): number {
    if (!this.ratings?.length) return 0;
    return (this.getStarCount(star) / this.ratings.length) * 100;
  }

  // ── Misc helpers ─────────────────────────────────────
  getInitial(name: string | null | undefined): string {
    if (!name?.trim()) return '✦';
    return name.trim().charAt(0).toUpperCase();
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return '';
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  getReviewRating(review: any): number {
  const match = this.ratings?.find(
    r => r.userId === review.userId && r.productId === review.productId
  );
  return match?.rating ?? 0;
}

  ngOnDestroy(): void {
    document.body.style.overflow = '';
  }
}
