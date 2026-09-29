import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// ─────────────────────────────────────────────────────────────────────────────
// jwtInterceptor 
//
// Header strategy per service:
//
//   Order Service  (/api/cart/*, /api/orders/*, /api/shipments/*)
//     → Authorization: Bearer {token}  only
//     → Backend resolves userId from JWT internally
//
//   Payment Service  (/payment/*)
//     → Authorization: Bearer {token}
//     → X-User-Id: {userId}     required on ALL payment endpoints
//     → X-User-Role: {role}     required on PUT /payment/{orderId}/status
//       (backend throws 401 UnauthorizedException if either is missing)
//
// Role value: stored as "CUSTOMER" or "ADMIN" in AuthService
// (login code strips the "ROLE_" prefix from the JWT claim)
// ─────────────────────────────────────────────────────────────────────────────

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router      = inject(Router);
  const token       = authService.getToken();

  const isPublicRoute = req.url.includes('/auth/login') ||
                        req.url.includes('/auth/signup');

  let authReq = req;

  if (token && !isPublicRoute) {
    const userId = authService.userId();
    const role   = authService.userRole();

    const headers: Record<string, string> = {
      Authorization: `Bearer ${token}`
    };

    if (userId !== null) headers['X-User-Id'] = String(userId);
    if (role)            headers['X-User-Role'] = role;

    authReq = req.clone({ setHeaders: headers });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        authService.logout();
        router.navigate(['/login']);
      }
      if (error.status === 403) {
        console.warn(`[jwtInterceptor] 403 on ${req.method} ${req.url}`);
      }
      return throwError(() => error);
    })
  );
};