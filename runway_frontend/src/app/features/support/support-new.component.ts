import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SupportService, TicketPriority } from '../../core/services/support.service';

@Component({
  selector: 'app-support-new',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './support-new.component.html',
  styleUrl: './support-new.component.css'
})
export class SupportNewComponent {
  private supportService = inject(SupportService);
  private router         = inject(Router);
  private route          = inject(ActivatedRoute);

  subject  = signal('');
  priority = signal<TicketPriority>('MEDIUM');
  orderId  = signal<string>('');
  submitting = signal(false);
  error      = signal<string | null>(null);

  readonly priorities: Array<{ value: TicketPriority; label: string; desc: string }> = [
    { value: 'LOW',      label: 'Low',      desc: 'General enquiry — no urgency' },
    { value: 'MEDIUM',   label: 'Medium',   desc: 'Requires attention within a few days' },
    { value: 'HIGH',     label: 'High',     desc: 'Time-sensitive matter' },
    { value: 'CRITICAL', label: 'Critical', desc: 'Immediate resolution required' },
  ];

  constructor() {
    // Pre-fill orderId if navigated from an order page
    const oid = this.route.snapshot.queryParamMap.get('orderId');
    if (oid) this.orderId.set(oid);
  }

  submit(): void {
    const subj = this.subject().trim();
    if (!subj) { this.error.set('Please describe the nature of your request.'); return; }

    this.submitting.set(true);
    this.error.set(null);

    const orderIdNum = this.orderId().trim()
      ? parseInt(this.orderId().trim(), 10) || null
      : null;

    this.supportService.createTicket({
      subject:  subj,
      priority: this.priority(),
      orderId:  orderIdNum
    }).subscribe({
      next:  (ticket) => this.router.navigate(['/support', ticket.id]),
      error: ()       => {
        this.error.set('Unable to submit your request. Please try again.');
        this.submitting.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/support']);
  }
}
