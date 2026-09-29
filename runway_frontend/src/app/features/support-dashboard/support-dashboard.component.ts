import { Component, inject, OnInit, signal, computed, ViewChild, ElementRef } from '@angular/core';
import { DatePipe, TitleCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';
import {
  groupNotificationsByDate,
  getEventLabel,
  NOTIF_FILTER_GROUPS,
} from '../../core/utils/notification.utils';

// ── Types ──────────────────────────────────────────────────────────────────────
type Section        = 'overview' | 'tickets' | 'notifications';
type TicketStatus   = 'OPEN' | 'IN_PROGRESS' | 'CLOSED';
type TicketFilter   = TicketStatus | 'ALL' | 'MINE';
type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
type SenderRole     = 'CUSTOMER' | 'SUPPORT' | 'ADMIN' | 'SELLER';
type NotifChannel   = 'EMAIL' | 'SMS' | 'PUSH';
type NotifStatus    = 'QUEUED' | 'SENT' | 'FAILED';

interface TicketResponse {
  id: number;
  userId: number;
  orderId: number | null;
  subject: string;
  priority: TicketPriority;
  status: TicketStatus;
  assignedTo: number | null;
  createdAt: string;
}

interface TicketMessageResponse {
  id: number;
  ticketId: number;
  senderId: number;
  senderRole: SenderRole;
  message: string;
  createdAt: string;
}

interface NotificationLogResponse {
  id: number;
  userId: number;
  event: string;
  channel: NotifChannel;
  status: NotifStatus;
  subject: string | null;
  content: string | null;
  payload: string | null;
  errorMessage: string | null;
  createdAt: string;
  sentAt: string | null;
}

interface CustomerInfo {
  name: string;
  email: string;
}

@Component({
  selector: 'app-support-dashboard',
  standalone: true,
  imports: [FormsModule, DatePipe, TitleCasePipe],
  templateUrl: './support-dashboard.component.html',
  styleUrl: './support-dashboard.component.css'
})
export class SupportDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private api         = inject(ApiService);

  @ViewChild('convScroll') convScroll?: ElementRef;

  // ── UI ─────────────────────────────────────────────────────────────────────
  activeSection = signal<Section>('overview');
  sidebarOpen   = signal(true);

  // ── Data ───────────────────────────────────────────────────────────────────
  tickets        = signal<TicketResponse[]>([]);
  notifications  = signal<NotificationLogResponse[]>([]);
  userDetails    = signal<any>(null);
  activeMessages = signal<TicketMessageResponse[]>([]);
  activeTicket   = signal<TicketResponse | null>(null);
  customerCache  = signal<Record<number, CustomerInfo>>({});

  // ── Loading ─────────────────────────────────────────────────────────────────
  loadingTickets       = signal(true);
  loadingNotifications = signal(true);
  loadingMessages      = signal(false);
  loadingUser          = signal(true);

  // ── Filters ────────────────────────────────────────────────────────────────
  ticketStatusFilter = signal<TicketFilter>('ALL');

  // ── Reply ──────────────────────────────────────────────────────────────────
  replyMessage = signal('');
  sendingReply = signal(false);

  // ── Notification ────────────────────────────────────────────────────────────
  expandedNotifId   = signal<number | null>(null);
  notifEventFilter  = signal<string>('ALL');
  readonly notifFilterGroups = NOTIF_FILTER_GROUPS;

  // ── Enums for template ──────────────────────────────────────────────────────
  readonly ticketStatuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'CLOSED'];

  // ── Computed ────────────────────────────────────────────────────────────────
  staffId  = computed(() => this.authService.userId());
  isAdmin  = computed(() => this.authService.userRole() === 'ADMIN');

  filteredTickets = computed(() => {
    const f = this.ticketStatusFilter();
    if (f === 'ALL')  return this.tickets();
    if (f === 'MINE') return this.tickets().filter(t => t.assignedTo === this.staffId());
    return this.tickets().filter(t => t.status === f);
  });

  openCount       = computed(() => this.tickets().filter(t => t.status === 'OPEN').length);
  inProgressCount = computed(() => this.tickets().filter(t => t.status === 'IN_PROGRESS').length);
  closedCount     = computed(() => this.tickets().filter(t => t.status === 'CLOSED').length);
  myTicketsCount  = computed(() => this.tickets().filter(t => t.assignedTo === this.staffId()).length);

  // Overview: unassigned OPEN tickets + my non-closed tickets
  overviewTickets = computed(() =>
    this.tickets().filter(t =>
      (t.status === 'OPEN' && !t.assignedTo) ||
      (t.status !== 'CLOSED' && t.assignedTo === this.staffId())
    )
  );

  filteredNotifications = computed(() => {
    const f = this.notifEventFilter();
    const list = this.notifications();
    return f === 'ALL' ? list : list.filter(n => n.event?.startsWith(f));
  });

  groupedNotifications = computed(() => groupNotificationsByDate(this.filteredNotifications()));

  ngOnInit(): void {
    this.loadUserDetails();
    this.loadTickets();
    this.loadNotifications();
  }

  // ── Data loading ─────────────────────────────────────────────────────────────
  private loadUserDetails(): void {
    const userId = this.staffId();
    if (!userId) return;
    this.authService.getUserDetails(userId).subscribe({
      next: (res) => { this.userDetails.set(res.user); this.loadingUser.set(false); },
      error: () => this.loadingUser.set(false)
    });
  }

  loadTickets(): void {
    this.loadingTickets.set(true);
    this.api.get<TicketResponse[]>('/tickets').subscribe({
      next: (data) => { this.tickets.set(data); this.loadingTickets.set(false); },
      error: () => this.loadingTickets.set(false)
    });
  }

  loadNotifications(): void {
    this.loadingNotifications.set(true);
    this.api.get<NotificationLogResponse[]>('/notification-logs').subscribe({
      next: (data) => { this.notifications.set(data); this.loadingNotifications.set(false); },
      error: () => this.loadingNotifications.set(false)
    });
  }

  // ── Customer details cache ───────────────────────────────────────────────────
  private fetchCustomerDetails(userId: number): void {
    if (this.customerCache()[userId]) return;
    this.authService.getUserDetails(userId).subscribe({
      next: (res) => {
        const u = res.user;
        this.customerCache.update(cache => ({
          ...cache,
          [userId]: { name: u?.name ?? `User #${userId}`, email: u?.email ?? '' }
        }));
      },
      error: () => {
        this.customerCache.update(cache => ({
          ...cache,
          [userId]: { name: `User #${userId}`, email: '' }
        }));
      }
    });
  }

  getCustomerName(userId: number): string {
    return this.customerCache()[userId]?.name ?? `User #${userId}`;
  }

  getCustomerEmail(userId: number): string {
    return this.customerCache()[userId]?.email ?? '';
  }

  // ── Ticket actions ───────────────────────────────────────────────────────────
  openTicket(ticket: TicketResponse): void {
    this.activeTicket.set(ticket);
    this.activeMessages.set([]);
    this.replyMessage.set('');
    this.fetchCustomerDetails(ticket.userId);
    this.loadMessages(ticket.id);
  }

  closeTicketView(): void {
    this.activeTicket.set(null);
    this.activeMessages.set([]);
  }

  private loadMessages(ticketId: number): void {
    this.loadingMessages.set(true);
    this.api.get<TicketMessageResponse[]>(`/tickets/${ticketId}/messages`).subscribe({
      next: (data) => {
        this.activeMessages.set(data);
        this.loadingMessages.set(false);
        this.scrollToBottom();
      },
      error: () => this.loadingMessages.set(false)
    });
  }

  /** Assigns ticket to this advisor, moves to IN_PROGRESS. Backend sends canned acknowledgment automatically. */
  acceptCase(ticket: TicketResponse): void {
    this.api.put<TicketResponse>(`/tickets/${ticket.id}?status=IN_PROGRESS`, {}).subscribe({
      next: (updated) => {
        this.tickets.update(ts => ts.map(t => t.id === ticket.id ? updated : t));
        if (this.activeTicket()?.id === ticket.id) {
          this.activeTicket.set(updated);
          this.loadMessages(ticket.id);
        }
      },
      error: () => {}
    });
  }

  /** Claim ticket and move to IN_PROGRESS — used from the ticket list quick-action. */
  claimTicket(ticket: TicketResponse): void {
    this.api.put<TicketResponse>(`/tickets/${ticket.id}?status=IN_PROGRESS`, {}).subscribe({
      next: (updated) => {
        this.tickets.update(ts => ts.map(t => t.id === ticket.id ? updated : t));
        if (this.activeTicket()?.id === ticket.id) this.activeTicket.set(updated);
      },
      error: () => {}
    });
  }

  updateStatus(ticket: TicketResponse, status: TicketStatus): void {
    this.api.put<TicketResponse>(`/tickets/${ticket.id}?status=${status}`, {}).subscribe({
      next: (updated) => {
        this.tickets.update(ts => ts.map(t => t.id === ticket.id ? updated : t));
        if (this.activeTicket()?.id === ticket.id) this.activeTicket.set(updated);
      },
      error: () => {}
    });
  }

  sendReply(): void {
    const ticket  = this.activeTicket();
    const message = this.replyMessage().trim();
    if (!ticket || !message) return;

    this.sendingReply.set(true);
    this.api.post<TicketMessageResponse>(`/tickets/${ticket.id}/messages`, { message }).subscribe({
      next: (msg) => {
        this.activeMessages.update(msgs => [...msgs, msg]);
        this.replyMessage.set('');
        this.sendingReply.set(false);
        this.scrollToBottom();
      },
      error: () => this.sendingReply.set(false)
    });
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.convScroll) {
        this.convScroll.nativeElement.scrollTop = this.convScroll.nativeElement.scrollHeight;
      }
    }, 60);
  }

  // ── Notification actions ─────────────────────────────────────────────────────
  toggleNotifExpand(id: number): void {
    this.expandedNotifId.update(cur => (cur === id ? null : id));
  }

  // ── Message helpers ──────────────────────────────────────────────────────────
  isSupportMsg(msg: TicketMessageResponse): boolean {
    return msg.senderRole === 'SUPPORT' || msg.senderRole === 'ADMIN';
  }

  isFirstInGroup(index: number): boolean {
    if (index === 0) return true;
    const msgs = this.activeMessages();
    return msgs[index].senderId !== msgs[index - 1].senderId;
  }

  isLastInGroup(index: number): boolean {
    const msgs = this.activeMessages();
    if (index === msgs.length - 1) return true;
    return msgs[index].senderId !== msgs[index + 1].senderId;
  }

  // ── Date helpers ─────────────────────────────────────────────────────────────
  /** Appends 'Z' when the backend returns a Java LocalDateTime with no timezone info. */
  normalizeDate(dateStr: string | null | undefined): Date | null {
    if (!dateStr) return null;
    const s = (dateStr.includes('Z') || dateStr.includes('+')) ? dateStr : dateStr + '+05:30';
    const d = new Date(s);
    return isNaN(d.getTime()) ? null : d;
  }

  // ── Navigation ───────────────────────────────────────────────────────────────
  setSection(section: Section): void {
    this.activeSection.set(section);
    this.activeTicket.set(null);
  }

  toggleSidebar(): void { this.sidebarOpen.update(v => !v); }

  logout(): void { this.authService.logout(); }

  // ── Helpers ──────────────────────────────────────────────────────────────────
  statusLabel(status: TicketStatus): string {
    const map: Record<TicketStatus, string> = {
      OPEN: 'Open', IN_PROGRESS: 'In Progress', CLOSED: 'Closed'
    };
    return map[status];
  }

  getPriorityClass(priority: TicketPriority): string {
    const map: Record<TicketPriority, string> = {
      LOW: 'priority-low', MEDIUM: 'priority-medium',
      HIGH: 'priority-high', CRITICAL: 'priority-critical'
    };
    return map[priority];
  }

  getStatusClass(status: TicketStatus): string {
    const map: Record<TicketStatus, string> = {
      OPEN: 'status-open', IN_PROGRESS: 'status-progress', CLOSED: 'status-closed'
    };
    return map[status];
  }

  getNotifStatusClass(status: NotifStatus): string {
    const map: Record<NotifStatus, string> = {
      QUEUED: 'status-queued', SENT: 'status-sent', FAILED: 'status-failed'
    };
    return map[status];
  }

  isMyTicket(ticket: TicketResponse): boolean {
    return ticket.assignedTo === this.staffId();
  }

  getEventLabel(event: string): string {
    return getEventLabel(event);
  }
}
