import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './unauthorized.component.html',
  styleUrl: './unauthorized.component.css'
})
export class UnauthorizedComponent {
  private auth = inject(AuthService);

  get isLoggedIn(): boolean { return this.auth.isLoggedIn(); }

  get dashboardRoute(): string {
    const role = this.auth.currentUser()?.role;
    if (role === 'SELLER')  return '/seller-dashboard';
    if (role === 'ADMIN')   return '/admin-dashboard';
    if (role === 'SUPPORT') return '/support-dashboard';
    return '/account';
  }

  get dashboardLabel(): string {
    const role = this.auth.currentUser()?.role;
    if (role === 'SELLER')  return 'Go to Seller Dashboard';
    if (role === 'ADMIN')   return 'Go to Admin Dashboard';
    if (role === 'SUPPORT') return 'Go to Support Dashboard';
    return 'Go to My Account';
  }
}
