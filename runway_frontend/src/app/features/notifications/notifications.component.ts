import { Component, inject, OnInit, computed } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService, NotificationLog } from '../../core/services/notification.service';
import { groupNotificationsByDate, getEventLabel as getLabel, getNotifRoute } from '../../core/utils/notification.utils';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.css',
})
export class Notifications implements OnInit {
  notifService = inject(NotificationService);
  private router = inject(Router);

  notifications = this.notifService.notifications;
  loading = this.notifService.loading;

  groupedNotifications = computed(() => groupNotificationsByDate(this.notifications()));

  ngOnInit(): void {
    this.notifService.load();
  }

  onNotifClick(n: NotificationLog): void {
    if (!n.readAt) this.notifService.markRead(n.id);
    const target = getNotifRoute(n.event, n.payload);
    if (target) this.router.navigate([target.path], { fragment: target.fragment });
  }

  getEventLabel(event: string): string {
    return getLabel(event);
  }

  getEventCategory(event: string): 'order' | 'payment' | 'review' | 'shipment' | 'ticket' | 'account' | 'default' {
    if (!event) return 'default';
    if (event.startsWith('ORDER'))   return 'order';
    if (event.startsWith('PAYMENT')) return 'payment';
    if (event.startsWith('REVIEW') || event.startsWith('RATING')) return 'review';
    if (event.startsWith('SHIPMENT')) return 'shipment';
    if (event.startsWith('TICKET'))  return 'ticket';
    if (event.startsWith('ACCOUNT')) return 'account';
    return 'default';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const s = dateStr.includes('Z') || dateStr.includes('+') ? dateStr : dateStr + 'Z';
    const d = new Date(s);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleString('en-IN', {
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
      timeZone: 'Asia/Kolkata'
    });
  }

  isNew(n: NotificationLog): boolean {
    return this.notifService.isNew(n);
  }

  goBack(): void {
    this.router.navigate(['/']);
  }
}
