import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { LoginRequest } from '../../../core/models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router      = inject(Router);
  private route       = inject(ActivatedRoute);

  /** True when the user just created an account and was redirected here from signup. */
  private readonly fromSignup  = history.state?.fromSignup === true;

  form: LoginRequest = {
    username: '',
    password: ''
  };
  
  loading = signal(false);
  error = signal<string | null>(null);
  showPassword = signal(false);

  onSubmit(): void {
    if (!this.form.username || !this.form.password) {
      this.error.set('Please fill in all fields.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authService.login(this.form).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.success) {
          setTimeout(() => {
            const role = this.authService.currentUser()?.role;
            if (role === 'SELLER') {
              this.router.navigate(['/seller-dashboard'], { replaceUrl: true });
            } else if (role === 'ADMIN') {
              this.router.navigate(['/admin-dashboard'], { replaceUrl: true });
            } else if (role === 'SUPPORT') {
              this.router.navigate(['/support-dashboard'], { replaceUrl: true });
            } else {
              if (this.fromSignup) {
                this.router.navigate(['/'], { replaceUrl: true });
              } else {
                const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
                returnUrl
                  ? this.router.navigateByUrl(returnUrl, { replaceUrl: true })
                  : window.history.back();
              }
            }
          }, 500);
        } else {
          this.error.set('Invalid credentials. Please try again.');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(
          err.status === 401
            ? 'Invalid username or password.'
            : 'Something went wrong. Please try again.'
        );
      }
    });
  }

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }
}