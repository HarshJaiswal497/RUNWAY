import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, switchMap, forkJoin } from 'rxjs';
import { AuthService, UserDetailsResponse } from './auth.service';

export interface Review {
  id: number;
  productId: number;
  userId: number;
  title: string;
  body: string;
  status: string;
  createdAt: string;
  userName?: string; // enriched field
}

@Injectable({
  providedIn: 'root'
})
export class ReviewsService {
  private baseUrl = 'http://localhost:9191';
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  readonly Math = Math;
  createReview(productId: number, title: string, body: string) {
    return this.http.post<Review>(`${this.baseUrl}/products/${productId}/reviews`, { productId, title, body });
  }

  getReviews(productId: number) {
    return this.http.get<Review[]>(`${this.baseUrl}/products/${productId}/reviews`).pipe(
      switchMap((reviews) => {
        // For each review, fetch user details
        const userCalls = reviews.map(r =>
          this.authService.getUserDetails(r.userId).pipe(
            map((resp: UserDetailsResponse) => ({
              ...r,
              userName: resp.user.name
            }))
          )
        );
        // Combine all calls
        return forkJoin(userCalls);
      })
    );
  }
}
