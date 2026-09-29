import { Injectable, inject, signal, computed } from '@angular/core';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';

export interface NotificationLog {
  id: number;
  userId: number;
  event: string;
  channel: 'EMAIL' | 'SMS' | 'PUSH';
  status: 'QUEUED' | 'SENT' | 'FAILED';
  subject: string;
  content: string;
  payload?: string;
  errorMessage?: string;
  createdAt: string;
  sentAt?: string;
  readAt?: string | null;
  read?: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private api  = inject(ApiService);
  private auth = inject(AuthService);

  private readonly POLL_MS = 30_000;

  notifications = signal<NotificationLog[]>([]);
  loading       = signal(false);

  unreadCount = computed(() => this.notifications().filter(n => !n.readAt).length);

  private pollTimer: ReturnType<typeof setInterval> | null = null;

  load(): void {
    if (!this.auth.isLoggedIn()) return;
    this.loading.set(true);
    this.api.get<NotificationLog[]>('/notification-logs', { mine: true }).subscribe({
      next: (logs) => { this.notifications.set(logs); this.loading.set(false); },
      error: ()    => { this.loading.set(false); }
    });
  }

  startPolling(): void {
    this.load();
    if (this.pollTimer) return;
    this.pollTimer = setInterval(() => {
      if (this.auth.isLoggedIn()) this.load();
    }, this.POLL_MS);
  }

  stopPolling(): void {
    if (this.pollTimer) { clearInterval(this.pollTimer); this.pollTimer = null; }
    this.notifications.set([]);
  }

  markRead(id: number): void {
    this.api.patch<void>(`/notification-logs/${id}/read`).subscribe({
      next: () => {
        const now = new Date().toISOString();
        this.notifications.update(list =>
          list.map(n => n.id === id ? { ...n, readAt: now, read: true } : n)
        );
      }
    });
  }

  markAllRead(): void {
    this.api.patch<void>('/notification-logs/read-all').subscribe({
      next: () => {
        const now = new Date().toISOString();
        this.notifications.update(list =>
          list.map(n => n.readAt ? n : { ...n, readAt: now, read: true })
        );
      }
    });
  }

  // kept for backwards compatibility (navbar dropdown still calls this)
  markAllSeen(): void {
    this.markAllRead();
  }

  isNew(n: NotificationLog): boolean {
    return !n.readAt;
  }
}
