import {
  Component, inject, signal, computed, OnInit, AfterViewChecked, ElementRef, ViewChild
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import {
  SupportService, Ticket, TicketMessage, TicketStatus
} from '../../core/services/support.service';

@Component({
  selector: 'app-support-detail',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './support-detail.component.html',
  styleUrl: './support-detail.component.css'
})
export class SupportDetailComponent implements OnInit, AfterViewChecked {
  @ViewChild('msgEnd') private msgEnd!: ElementRef;

  private supportService = inject(SupportService);
  readonly authService   = inject(AuthService);
  private route          = inject(ActivatedRoute);

  ticket    = signal<Ticket | null>(null);
  messages  = signal<TicketMessage[]>([]);
  loading   = signal(true);
  sending   = signal(false);
  error     = signal<string | null>(null);
  replyText = signal('');

  private ticketId = 0;
  private shouldScroll = false;

  isStaff = computed(() => {
    const role = this.authService.userRole();
    return role === 'ADMIN' || role === 'SUPPORT';
  });

  isClosed = computed(() => this.ticket()?.status === 'CLOSED');

  readonly statusOptions: Array<{ value: TicketStatus; label: string }> = [
    { value: 'OPEN',        label: 'Open'        },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'CLOSED',      label: 'Resolve'     },
  ];

  ngOnInit(): void {
    this.ticketId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAll();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  private loadAll(): void {
    this.loading.set(true);
    this.supportService.getTicket(this.ticketId).subscribe({
      next: (t) => {
        this.ticket.set(t);
        this.loadMessages();
      },
      error: () => {
        this.error.set('Unable to load this request. Please try again.');
        this.loading.set(false);
      }
    });
  }

  private loadMessages(): void {
    this.supportService.getMessages(this.ticketId).subscribe({
      next: (msgs) => {
        // Always prepend a synthetic acknowledgment as the first message so the
        // customer sees immediate context on every open — even after real replies arrive.
        const welcome: TicketMessage = {
          id:         -1,
          ticketId:   this.ticketId,
          senderId:   -1,   // never matches any userId → always renders as advisor bubble
          senderRole: 'SUPPORT',
          message:
            'Thank you for contacting RunWay Client Services.\n\n' +
            'Your request has been received and assigned a dedicated reference. ' +
            'Our advisors review every enquiry personally — you can expect a response within 24 hours.\n\n' +
            'Should you wish to provide any additional context, please use the message field below.',
          createdAt: this.ticket()?.createdAt ?? new Date().toISOString()
        };
        this.messages.set([welcome, ...msgs]);
        this.loading.set(false);
        this.shouldScroll = true;
      },
      error: () => { this.loading.set(false); }
    });
  }

  sendReply(): void {
    const text = this.replyText().trim();
    if (!text || this.sending()) return;

    this.sending.set(true);
    this.supportService.sendMessage(this.ticketId, text).subscribe({
      next: (msg) => {
        this.messages.update(m => [...m, msg]);
        this.replyText.set('');
        this.sending.set(false);
        this.shouldScroll = true;
      },
      error: () => { this.sending.set(false); }
    });
  }

  // Staff: accept + move to IN_PROGRESS in one call
  acceptCase(): void {
    this.supportService.updateTicket(this.ticketId, 'IN_PROGRESS').subscribe({
      next: (t) => this.ticket.set(t)
    });
  }

  changeStatus(status: TicketStatus): void {
    if (status === this.ticket()?.status) return;
    this.supportService.updateTicket(this.ticketId, status).subscribe({
      next: (t) => this.ticket.set(t)
    });
  }

  isOwnMessage(msg: TicketMessage): boolean {
    return msg.senderId === this.authService.userId();
  }

  isSupportMessage(msg: TicketMessage): boolean {
    return msg.senderRole === 'SUPPORT' || msg.senderRole === 'ADMIN';
  }

  senderLabel(msg: TicketMessage): string {
    return this.isSupportMessage(msg) ? 'RunWay Advisor' : 'You';
  }

  // Group consecutive messages from the same sender
  isFirstInGroup(index: number): boolean {
    if (index === 0) return true;
    const msgs = this.messages();
    return msgs[index].senderId !== msgs[index - 1].senderId;
  }

  isLastInGroup(index: number): boolean {
    const msgs = this.messages();
    if (index === msgs.length - 1) return true;
    return msgs[index].senderId !== msgs[index + 1].senderId;
  }

  priorityLabel(p: string): string {
    return { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', CRITICAL: 'Critical' }[p] ?? p;
  }

  statusLabel(s: string): string {
    return { OPEN: 'Open', IN_PROGRESS: 'In Progress', CLOSED: 'Resolved' }[s] ?? s;
  }

  priorityClass(p: string): string { return 'priority-' + p?.toLowerCase(); }
  statusClass(s: string):   string { return 'status-'   + s?.toLowerCase(); }

  formatTime(d: string): string {
    if (!d) return '';
    const dt = new Date(d.includes('Z') || d.includes('+') ? d : d + '+05:30');
    return isNaN(dt.getTime()) ? '' : dt.toLocaleString('en-IN', {
      day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
      hour12: true, timeZone: 'Asia/Kolkata'
    });
  }

  onReplyKey(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendReply();
    }
  }

  private scrollToBottom(): void {
    try { this.msgEnd?.nativeElement?.scrollIntoView({ behavior: 'smooth' }); } catch {}
  }
}
