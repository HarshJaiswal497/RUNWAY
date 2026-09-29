import { Component, Input, Output, EventEmitter, OnDestroy, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { switchMap } from 'rxjs';
import { ReviewsService } from '../../core/services/reviews.service';
import { RatingService } from '../../core/services/rating.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-create-reviews',
  imports: [],
  standalone: true,
  templateUrl: './create-reviews.component.html',
  styleUrl: './create-reviews.component.css',
})
export class CreateReviews implements OnDestroy {

  @Input() productId: number | null = null;
  @Output() reviewSubmitted = new EventEmitter<void>();

  readonly starRange = [1, 2, 3, 4, 5];

  private reviewsService = inject(ReviewsService);
  private ratingService  = inject(RatingService);
  private authService    = inject(AuthService);

  // ── Modal state ───────────────────────────────────────
  modalOpen   = false;
  submitted   = false;
  submitting  = false;
  submitError = '';

  // ── Form state ────────────────────────────────────────
  selectedRating = 0;
  hoverRating    = 0;
  reviewTitle    = '';
  reviewBody     = '';

  openModal(): void {
    this.modalOpen   = true;
    this.submitted   = false;
    this.submitError = '';
    document.body.style.overflow = 'hidden';
  }

  closeModal(): void {
    this.modalOpen = false;
    this.resetForm();
    document.body.style.overflow = '';
  }

  private resetForm(): void {
    this.selectedRating = 0;
    this.hoverRating    = 0;
    this.reviewTitle    = '';
    this.reviewBody     = '';
    this.submitError    = '';
    this.submitted      = false;
  }

  getRatingLabel(n: number): string {
    return ['', 'Disappointing', 'Below Expectations', 'Satisfactory', 'Impressive', 'Exceptional'][n] ?? '';
  }

  get canSubmit(): boolean {
    return (
      this.selectedRating > 0 &&
      this.reviewTitle.trim().length > 0 &&
      this.reviewBody.trim().length > 0 &&
      !this.submitting
    );
  }

  submitReview(): void {
    if (!this.productId || !this.canSubmit) return;

    const token = this.authService.getToken();
    // console.log('[CreateReviews] Submitting review', {
    //   productId: this.productId,
    //   rating: this.selectedRating,
    //   title: this.reviewTitle.trim(),
    //   bodyLength: this.reviewBody.trim().length,
    //   isLoggedIn: this.authService.isLoggedIn(),
    //   userId: this.authService.userId(),
    //   tokenPresent: !!token,
    //   tokenPreview: token ? token.substring(0, 30) + '...' : 'NULL'
    // });

    this.submitting  = true;
    this.submitError = '';

    // Post review first (has the purchase-verification check).
    // Only if that succeeds do we post the rating, preventing orphaned ratings.
    this.reviewsService.createReview(
      this.productId,
      this.reviewTitle.trim(),
      this.reviewBody.trim()
    ).pipe(
      switchMap((reviewResp) => {
        // console.log('[CreateReviews] Review POST succeeded', reviewResp);
        return this.ratingService.createRating(this.productId!, this.selectedRating);
      })
    ).subscribe({
      next: (ratingResp) => {
        // console.log('[CreateReviews] Rating POST succeeded', ratingResp);
        this.submitting = false;
        this.submitted  = true;
        this.reviewSubmitted.emit();
      },
      error: (err: HttpErrorResponse) => {
        console.error('[CreateReviews] Submission failed', {
          status: err.status,
          statusText: err.statusText,
          error: err.error,
          url: err.url
        });
        this.submitting = false;
        if (err.status === 401) {
          this.submitError = 'Please sign in to share your experience with this piece.';
        } else if (err.status === 403) {
          this.submitError = 'Only verified purchasers of this piece may share their experience.';
        } else if (err.status === 409) {
          this.submitError = 'You have already submitted a review for this piece.';
        } else {
          this.submitError = 'We were unable to submit your review at this time. Please try again momentarily.';
        }
      }
    });
  }

  ngOnDestroy(): void {
    document.body.style.overflow = '';
  }
}
