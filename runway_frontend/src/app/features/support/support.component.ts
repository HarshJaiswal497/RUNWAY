import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SupportService, Ticket, TicketStatus } from '../../core/services/support.service';

@Component({
  selector: 'app-support',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './support.component.html',
  styleUrl: './support.component.css'
})
export class SupportComponent implements OnInit {
  private supportService = inject(SupportService);
  readonly authService   = inject(AuthService);
  private router         = inject(Router);

  tickets  = signal<Ticket[]>([]);
  loading  = signal(true);
  error    = signal<string | null>(null);
  filter   = signal<TicketStatus | 'ALL'>('ALL');

  isStaff = computed(() => {
    const role = this.authService.userRole();
    return role === 'ADMIN' || role === 'SUPPORT';
  });

  filtered = computed(() => {
    const f = this.filter();
    if (f === 'ALL') return this.tickets();
    return this.tickets().filter(t => t.status === f);
  });

  readonly filters: Array<{ value: TicketStatus | 'ALL'; label: string }> = [
    { value: 'ALL',         label: 'All'         },
    { value: 'OPEN',        label: 'Open'        },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'CLOSED',      label: 'Resolved'    },
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    const req$ = this.isStaff()
      ? this.supportService.getAllTickets()
      : this.supportService.getMyTickets();

    req$.subscribe({
      next:  (data) => { this.tickets.set(data); this.loading.set(false); },
      error: ()     => { this.error.set('Unable to load requests. Please try again.'); this.loading.set(false); }
    });
  }

  openTicket(id: number): void {
    this.router.navigate(['/support', id]);
  }

  raiseRequest(): void {
    this.router.navigate(['/support/new']);
  }

  setFilter(f: TicketStatus | 'ALL'): void {
    this.filter.set(f);
  }

  countByStatus(status: TicketStatus | 'ALL'): number {
    if (status === 'ALL') return this.tickets().length;
    return this.tickets().filter(t => t.status === status).length;
  }

  priorityClass(p: string): string {
    return 'priority-' + p.toLowerCase();
  }

  statusClass(s: string): string {
    return 'status-' + s.toLowerCase();
  }

  priorityLabel(p: string): string {
    return { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', CRITICAL: 'Critical' }[p] ?? p;
  }

  statusLabel(s: string): string {
    return { OPEN: 'Open', IN_PROGRESS: 'In Progress', CLOSED: 'Resolved' }[s] ?? s;
  }

  formatDate(d: string): string {
    if (!d) return '';
    const dt = new Date(d.includes('Z') || d.includes('+') ? d : d + '+05:30');
    return isNaN(dt.getTime()) ? '' : dt.toLocaleDateString('en-IN', {
      day: 'numeric', month: 'short', year: 'numeric', timeZone: 'Asia/Kolkata'
    });
  }
}
