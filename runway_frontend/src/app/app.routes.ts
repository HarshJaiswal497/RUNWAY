import { Routes } from '@angular/router';
import { sellerGuard, adminGuard } from './core/guards/role.guard';
import { customerGuard } from './core/guards/customer.guard';
import { authGuard } from './core/guards/auth.guard';
import { supportGuard } from './core/guards/support.guard';

export const routes: Routes = [

  // ── Home ──────────────────────────────────────────────────────────────────
  {
    path: '',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'info',
    loadComponent: () =>
      import('./features/info/info.component')
        .then(m => m.InfoComponent)
  },
  {
    path: 'our-story',
    loadComponent: () =>
      import('./features/our-story/our-story.component')
        .then(m => m.OurStoryComponent)
  },
  {
    path: 'lookbook',
    loadComponent: () =>
      import('./features/lookbook/lookbook.component')
        .then(m => m.LookbookComponent)
  },

  // ── Auth ──────────────────────────────────────────────────────────────────
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component')
        .then(m => m.LoginComponent)
  },
  {
    path: 'signup',
    loadComponent: () =>
      import('./features/auth/signup/signup.component')
        .then(m => m.SignupComponent)
  },

  // ── Dashboards ────────────────────────────────────────────────────────────
  {
    path: 'seller-dashboard',
    canActivate: [sellerGuard],
    loadComponent: () =>
      import('./features/seller-dashboard/seller-dashboard.component')
        .then(m => m.SellerDashboardComponent)
  },
  {
    path: 'admin-dashboard',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin-dashboard/admin-dashboard.component')
        .then(m => m.AdminDashboardComponent)
  },
  {
    path: 'support-dashboard',
    canActivate: [supportGuard],
    loadComponent: () =>
      import('./features/support-dashboard/support-dashboard.component')
        .then(m => m.SupportDashboardComponent)
  },

  // ── Catalog ───────────────────────────────────────────────────────────────
  {
    path: 'category/all',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/category/category.component')
        .then(m => m.CategoryComponent)
  },
  {
    path: 'category/new',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/category/category.component')
        .then(m => m.CategoryComponent)
  },
  {
    path: 'category/seller/:sellerId',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/category/category.component')
        .then(m => m.CategoryComponent)
  },
  {
    path: 'category/:id',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/category/category.component')
        .then(m => m.CategoryComponent)
  },
  {
    path: 'product/:id',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/product-detail/product-detail.component')
        .then(m => m.ProductDetailComponent)
  },
  {
    path: 'search',
    canActivate: [customerGuard],
    loadComponent: () =>
      import('./features/search/search.component')
        .then(m => m.SearchComponent)
  },
  {
    path: 'wishlist',
    canActivate: [authGuard, customerGuard],
    loadComponent: () =>
      import('./features/wishlist/wishlist.component')
        .then(m => m.WishlistComponent)
  },

  // ── Order Service ─────────────────────────────────────────────────────────
  {
    path: 'cart',
    canActivate: [authGuard, customerGuard],
    loadComponent: () =>
      import('./features/cart/cart.component')
        .then(m => m.CartComponent)
  },
  {
    path: 'checkout',
    canActivate: [authGuard, customerGuard],
    loadComponent: () =>
      import('./features/checkout/checkout.component')
        .then(m => m.CheckoutComponent)
  },
  {
    path: 'orders',
    canActivate: [authGuard, customerGuard],
    loadComponent: () =>
      import('./features/my-orders/my-orders.component')
        .then(m => m.MyOrdersComponent)
  },
  {
    path: 'orders/:id/tracking',
    canActivate: [authGuard, customerGuard],
    loadComponent: () =>
      import('./features/tracking/tracking.component')
        .then(m => m.TrackingComponent)
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard, customerGuard],
    loadComponent: () =>
      import('./features/order-detail/order-detail.component')
        .then(m => m.OrderDetailComponent)
  },

  // ── Account ───────────────────────────────────────────────────────────────
  {
    path: 'account',
    canActivate: [authGuard, customerGuard],
    loadComponent: () =>
      import('./features/account/account.component')
        .then(m => m.AccountComponent)
  },
  // ── Notifications ───────────────────────────────────────────────────────
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/notifications/notifications.component')
        .then(m => m.Notifications)
  },

  // ── Client Services ───────────────────────────────────────────────────────
  // IMPORTANT: support/new must come BEFORE support/:id
  {
    path: 'support',
    loadComponent: () =>
      import('./features/support/support.component')
        .then(m => m.SupportComponent)
  },
  {
    path: 'support/new',
    loadComponent: () =>
      import('./features/support/support-new.component')
        .then(m => m.SupportNewComponent)
  },
  {
    path: 'support/:id',
    loadComponent: () =>
      import('./features/support/support-detail.component')
        .then(m => m.SupportDetailComponent)
  },

  // ── Error pages ───────────────────────────────────────────────────────────
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./features/unauthorized/unauthorized.component')
        .then(m => m.UnauthorizedComponent)
  },
  {
    path: '**',
    loadComponent: () =>
      import('./features/not-found/not-found.component')
        .then(m => m.NotFoundComponent)
  }
];