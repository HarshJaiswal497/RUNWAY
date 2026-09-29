import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  submessage?: string;
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  toasts = signal<Toast[]>([]);
  private counter = 0;

  show(message: string, submessage?: string, duration = 2500): void {
    const id = ++this.counter;
    this.toasts.update(ts => [...ts, { id, message, submessage, duration }]);
    setTimeout(() => this.dismiss(id), duration);
  }

  dismiss(id: number): void {
    this.toasts.update(ts => ts.filter(t => t.id !== id));
  }
}