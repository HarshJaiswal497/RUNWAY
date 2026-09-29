import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface Rating {
  productId: number;
  userId: number;
  rating: number;
}

export interface AverageRating {
  average: number;
  count: number;
}

export interface Review {
  id: number;
  productId: number;
  userId: number;
  content: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class RatingService {
  private api = inject(ApiService);

  // ── Ratings ───────────────────────────────────────

  getProductRatings(productId: number): Observable<Rating[]> {
    return this.api.get<Rating[]>(`/products/${productId}/ratings`);
  }

  getAverageRating(productId: number): Observable<AverageRating> {
    return this.api.get<AverageRating>(`/products/${productId}/ratings/average`);
  }

  createRating(productId: number, rating: number): Observable<Rating> {
    return this.api.post<Rating>(`/products/${productId}/ratings`, { rating });
  }

  updateRating(productId: number, rating: number): Observable<Rating> {
    return this.api.put<Rating>(`/products/${productId}/ratings`, { rating });
  }

  deleteRating(productId: number): Observable<void> {
    return this.api.delete<void>(`/products/${productId}/ratings`);
  }

  // ── Reviews ───────────────────────────────────────

  getProductReviews(productId: number): Observable<Review[]> {
    return this.api.get<Review[]>(`/products/${productId}/reviews`);
  }

  createReview(productId: number, content: string): Observable<Review> {
    return this.api.post<Review>(`/products/${productId}/reviews`, { content });
  }

  getReviewById(id: number): Observable<Review> {
    return this.api.get<Review>(`/reviews/${id}`);
  }

  updateReview(id: number, content: string): Observable<Review> {
    return this.api.put<Review>(`/reviews/${id}`, { content });
  }

  deleteReview(id: number): Observable<void> {
    return this.api.delete<void>(`/reviews/${id}`);
  }
}