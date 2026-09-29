import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './footer.html',
  styleUrl: './footer.css'
})
export class FooterComponent {
  private toast = inject(ToastService);
  year = new Date().getFullYear();

  onSocial(platform: string): void {
    this.toast.show(
      'Coming Soon',
      `RunWay on ${platform} — we'll be there shortly.`
    );
  }

  onSubscribe(): void {
  this.toast.show(
    'Thank You',
    "You'll be the first to know when we launch our newsletter."
  );
}
}