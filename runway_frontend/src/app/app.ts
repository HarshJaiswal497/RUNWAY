import { Component, signal, inject } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { NavbarComponent } from './shared/navbar/navbar';
import { FooterComponent } from './shared/footer/footer';
import { ToastComponent } from './shared/toast/toast.component';
import { filter } from 'rxjs';

const NO_FOOTER_ROUTES = [/^\/support\/\d+$/, /^\/account/];

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent, ToastComponent],
  templateUrl: './app.html'
})
export class App {
  private router = inject(Router);

  showShell  = signal(true);
  showFooter = signal(true);

  private dashboardRoutes = [
    '/seller-dashboard',
    '/admin-dashboard',
    '/support-dashboard'
  ];

  constructor() {
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: any) => {
      const url = e.urlAfterRedirects as string;
      const isDashboard = this.dashboardRoutes.some(r => url.startsWith(r));
      this.showShell.set(!isDashboard);
      this.showFooter.set(!isDashboard && !NO_FOOTER_ROUTES.some(re => re.test(url)));
    });
  }
}