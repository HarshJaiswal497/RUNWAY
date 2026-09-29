import { Injectable, inject, signal, computed } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { ApiService } from './api.service';
import {
  LoginRequest,
  LoginResponse,
  SignupRequest,
  LoggedInUser
} from '../models/auth.model';

export interface UserDetails {
  user_id: number;
  name: string;
  username: string;
  email: string;
  gender: string;
  roles: string[];
}

export interface UserDetailsResponse {
  success: boolean;
  user: UserDetails;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api    = inject(ApiService);
  private router = inject(Router);

  // ── Current user state ────────────────────────────
  currentUser = signal<LoggedInUser | null>(this.loadFromStorage());
  isLoggedIn  = computed(() => !!this.currentUser());
  userRole    = computed(() => this.currentUser()?.role ?? null);
  userId      = computed(() => this.currentUser()?.userId ?? null);

  // ── Auth API calls ────────────────────────────────
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.api.post<LoginResponse>('/auth/login', request).pipe(
      tap((response) => {
        if (response.success) {
          // Fetch user details after login to get username + role
          this.getUserDetails(response.userId).subscribe({
            next: (details) => {
              const user: LoggedInUser = {
                userId:   response.userId,
                username: details.user.username,
                token:    response.token,
                role:     details.user.roles[0]?.replace('ROLE_', '') ?? 'CUSTOMER'
              };
              this.currentUser.set(user);
              this.saveToStorage(user);
            }
          });
        }
      })
    );
  }

  signup(request: SignupRequest): Observable<any> {
    return this.api.post<any>('/auth/signup', request);
  }

  logout(): void {
    this.currentUser.set(null);
    localStorage.removeItem('runway_user');
    this.router.navigate(['/']);
  }

  getUserDetails(userId: number): Observable<UserDetailsResponse> {
    return this.api.get<UserDetailsResponse>('/auth/userdetails', { userId });
  }

  getSupportUsers(): Observable<UserDetailsResponse[]> {
    return this.api.get<UserDetailsResponse[]>('/auth/users', { filterRole: 'SUPPORT' });
  }

  // ── Storage helpers ───────────────────────────────
  private saveToStorage(user: LoggedInUser): void {
    localStorage.setItem('runway_user', JSON.stringify(user));
  }

  private loadFromStorage(): LoggedInUser | null {
    const stored = localStorage.getItem('runway_user');
    return stored ? JSON.parse(stored) : null;
  }

  getToken(): string | null {
    return this.currentUser()?.token ?? null;
  }
}