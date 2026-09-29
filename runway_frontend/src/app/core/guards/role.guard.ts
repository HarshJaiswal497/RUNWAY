import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const sellerGuard: CanActivateFn = (_route, state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  const role = auth.currentUser()?.role;
  if (role === 'SELLER' || role === 'ADMIN') return true;

  router.navigate(['/unauthorized']);
  return false;
};

export const adminGuard: CanActivateFn = (_route, state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  if (auth.currentUser()?.role === 'ADMIN') return true;

  router.navigate(['/unauthorized']);
  return false;
};