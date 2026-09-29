import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const customerGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) return true;

  const role = auth.currentUser()?.role;
  if (role === 'SELLER') {
    router.navigate(['/seller-dashboard'], { replaceUrl: true });
    return false;
  }
  if (role === 'ADMIN') {
    router.navigate(['/admin-dashboard'], { replaceUrl: true });
    return false;
  }
  if (role === 'SUPPORT') {
    router.navigate(['/support-dashboard'], { replaceUrl: true });
    return false;
  }

  return true;
};