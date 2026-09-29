import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

// ── Domain types ──────────────────────────────────────────────────────────────
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketStatus   = 'OPEN' | 'IN_PROGRESS' | 'CLOSED';
export type SenderRole     = 'CUSTOMER' | 'SUPPORT' | 'ADMIN' | 'SELLER';

export interface Ticket {
  id:         number;
  userId:     number;
  orderId?:   number;
  subject:    string;
  priority:   TicketPriority;
  status:     TicketStatus;
  assignedTo?: number;
  createdAt:  string;
}

export interface TicketMessage {
  id:         number;
  ticketId:   number;
  senderId:   number;
  senderRole: SenderRole;
  message:    string;
  createdAt:  string;
}

export interface TicketRequest {
  subject:  string;
  priority: TicketPriority;
  orderId?: number | null;
}

// ── Service ───────────────────────────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class SupportService {
  private api  = inject(ApiService);
  private http = inject(HttpClient);

  // CUSTOMER — own tickets only
  getMyTickets(): Observable<Ticket[]> {
    return this.api.get<Ticket[]>('/tickets', { mine: true });
  }

  // ADMIN / SUPPORT — all tickets, optionally filtered by status
  getAllTickets(status?: TicketStatus): Observable<Ticket[]> {
    const params: Record<string, any> = {};
    if (status) params['status'] = status;
    return this.api.get<Ticket[]>('/tickets', params);
  }

  getTicket(id: number): Observable<Ticket> {
    return this.api.get<Ticket>(`/tickets/${id}`);
  }

  createTicket(req: TicketRequest): Observable<Ticket> {
    return this.api.post<Ticket>('/tickets', req);
  }

  // ADMIN / SUPPORT — update status and/or claim the case
  // Passing no status → assigns to caller only (claim)
  // Passing a status  → updates status + assigns if unclaimed
  updateTicket(id: number, status?: TicketStatus): Observable<Ticket> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.put<Ticket>(
      `${this.api.baseUrl}/tickets/${id}`,
      {},
      { params }
    );
  }

  getMessages(ticketId: number): Observable<TicketMessage[]> {
    return this.api.get<TicketMessage[]>(`/tickets/${ticketId}/messages`);
  }

  sendMessage(ticketId: number, message: string): Observable<TicketMessage> {
    return this.api.post<TicketMessage>(`/tickets/${ticketId}/messages`, { message });
  }
}
