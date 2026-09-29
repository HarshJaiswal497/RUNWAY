import { Component, inject, OnInit, signal, computed, ViewChild, ElementRef } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RwSelectComponent } from '../../shared/rw-select/rw-select.component';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';

type Section = 'overview' | 'users' | 'orders' | 'reviews' | 'tickets' | 'notifications';
type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED';
type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'CLOSED';
type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
type NotifStatus = 'QUEUED' | 'SENT' | 'FAILED';
type NotifChannel = 'EMAIL' | 'SMS' | 'PUSH';
type SenderRole = 'CUSTOMER' | 'SUPPORT' | 'ADMIN' | 'SELLER';

interface User {
    user_id: number;
    name: string;
    username: string;
    email: string;
    gender: string;
    roles: string[];
}

interface OrderLineResponse {
    id: number;
    productId: number;
    productTitle: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
}

interface OrderResponse {
    id: number;
    orderNumber: string;
    userId: number;
    status: OrderStatus;
    totalAmount: number;
    currency: string;
    shippingAddress: string;
    notes: string;
    lines: OrderLineResponse[];
    createdAt: string;
    updatedAt: string;
}

interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    number: number;
    size: number;
}

interface ApiPageResponse<T> {
    success: boolean;
    message: string;
    data: PageResponse<T>;
}

interface ReviewResponse {
    id: number;
    title: string;
    body: string;
    status: ReviewStatus;
    productId: number;
    userId: number;
    createdAt: string;
}

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
    subject: string | null;
    content: string | null;
    payload: string | null;
    errorMessage: string | null;
    channel: NotifChannel;
    status: NotifStatus;
    sentAt: string | null;
    createdAt: string;
}

@Component({
    selector: 'app-admin-dashboard',
    standalone: true,
    imports: [FormsModule, DatePipe, DecimalPipe, RwSelectComponent],
    templateUrl: './admin-dashboard.component.html',
    styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
    private authService = inject(AuthService);
    private api = inject(ApiService);

    @ViewChild('convScroll') convScroll?: ElementRef;

    // ── UI ────────────────────────────────────────────
    activeSection = signal<Section>('overview');
    sidebarOpen = signal(true);
    userDetails = signal<any>(null);

    // ── Users ──────────────────────────────────────────
    users = signal<User[]>([]);
    loadingUsers = signal(true);
    userSearch = signal('');
    showOnboardSellerModal = signal(false);
    showOnboardSupportModal = signal(false);
    showOffboardModal = signal(false);
    showDeleteUserModal = signal(false);
    selectedUser = signal<User | null>(null);
    onboardUsername = signal('');
    modalLoading = signal(false);
    modalError = signal<string | null>(null);
    modalSuccess = signal<string | null>(null);

    // ── Orders ─────────────────────────────────────────
    orders = signal<OrderResponse[]>([]);
    loadingOrders = signal(true);
    orderPage = signal(0);
    orderTotalPages = signal(0);
    orderTotal = signal(0);
    selectedOrder = signal<OrderResponse | null>(null);
    showOrderModal = signal(false);
    newOrderStatus = signal<OrderStatus>('PAID');

    // ── Reviews ────────────────────────────────────────
    reviews = signal<ReviewResponse[]>([]);
    loadingReviews = signal(true);
    reviewFilter = signal<ReviewStatus | 'ALL'>('ALL');

    // ── Tickets ────────────────────────────────────────
    tickets = signal<TicketResponse[]>([]);
    loadingTickets = signal(true);
    ticketFilter = signal<TicketStatus | 'ALL'>('ALL');
    activeTicket = signal<TicketResponse | null>(null);
    activeMessages = signal<TicketMessageResponse[]>([]);
    loadingMessages = signal(false);
    replyMessage = signal('');
    sendingReply = signal(false);

    // ── Notifications ──────────────────────────────────
    notifications = signal<NotificationLogResponse[]>([]);
    loadingNotifications = signal(true);
    expandedNotifId = signal<number | null>(null);
    notifEventFilter = signal<string>('ALL');

    // ── Ticket assignment ──────────────────────────────
    selectedAssignee = signal<number | null>(null);
    assigningTicket = signal(false);
    assignSuccess = signal<string | null>(null);
    assignError = signal<string | null>(null);
    supportUsersForAssign = signal<any[]>([]);

    // ── Enums ──────────────────────────────────────────
    readonly orderStatuses: OrderStatus[] = [
        'PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED',
        'DELIVERED', 'COMPLETED', 'CANCELLED', 'REFUNDED'
    ];

    // ── rw-select option lists ──────────────────────────
    get supportUserOpts() {
        return this.supportUsersForAssign().map(a => ({
            value: a.user_id,
            label: `${a.name} (#${a.user_id})`
        }));
    }
    get orderStatusOpts()    { return this.orderStatuses.map(s => ({ value: s, label: s })); }
    get shipmentStatusOpts() { return this.shipmentStatuses.map(s => ({ value: s, label: s })); }
    readonly ticketStatuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'CLOSED'];
    readonly notifFilterGroups = [
        { label: 'All',       value: 'ALL' },
        { label: 'Orders',    value: 'ORDER' },
        { label: 'Shipments', value: 'SHIPMENT' },
        { label: 'Payments',  value: 'PAYMENT' },
        { label: 'Reviews',   value: 'REVIEW' },
        { label: 'Tickets',   value: 'TICKET' },
        { label: 'Ratings',   value: 'RATING' },
        { label: 'Account',   value: 'ACCOUNT' },
    ];
    readonly navItems: { key: Section; label: string }[] = [
        { key: 'overview', label: 'Overview' },
        { key: 'users', label: 'Users' },
        { key: 'orders', label: 'Orders' },
        { key: 'reviews', label: 'Reviews' },
        { key: 'tickets', label: 'Requests' },
        { key: 'notifications', label: 'Notifications' },
    ];

    // ── Computed ───────────────────────────────────────
    adminId = computed(() => this.authService.userId());

    // ── Shipment ───────────────────────────────────────
    activeShipment = signal<any>(null);
    loadingShipment = signal(false);
    newShipmentStatus = signal<string>('DISPATCHED');
    trackingForm = { carrier: '', trackingNumber: '' };

    readonly shipmentStatuses = [
        'PROCESSING', 'DISPATCHED', 'IN_TRANSIT',
        'OUT_FOR_DELIVERY', 'DELIVERED', 'RETURNED'
    ];

    // Update viewOrder to also load shipment
    viewOrder(order: OrderResponse): void {
        this.selectedOrder.set(order);
        this.newOrderStatus.set(order.status);
        this.showOrderModal.set(true);
        this.modalError.set(null);
        this.modalSuccess.set(null);
        this.activeShipment.set(null);
        this.loadShipment(order.id);
    }

    private loadShipment(orderId: number): void {
        this.loadingShipment.set(true);
        this.api.get<any>(`/api/shipments/order/${orderId}`).subscribe({
            next: (res) => {
                this.activeShipment.set(res.data ?? res);
                this.loadingShipment.set(false);
                if (this.activeShipment()) {
                    this.newShipmentStatus.set(this.activeShipment().status);
                    this.trackingForm.carrier = this.activeShipment().carrier ?? '';
                    this.trackingForm.trackingNumber = this.activeShipment().trackingNumber ?? '';
                }
            },
            error: () => {
                this.activeShipment.set(null);
                this.loadingShipment.set(false);
            }
        });
    }

    updateShipmentStatus(): void {
        const shipment = this.activeShipment();
        if (!shipment) return;
        this.api.put<any>(`/api/shipments/${shipment.id}/status`, {
            status: this.newShipmentStatus()
        }).subscribe({
            next: (res) => {
                this.activeShipment.set(res.data ?? res);
                this.modalSuccess.set('Shipment status updated!');
                setTimeout(() => this.modalSuccess.set(null), 2000);
            },
            error: () => this.modalError.set('Failed to update shipment status.')
        });
    }

    assignTracking(): void {
        const shipment = this.activeShipment();
        if (!shipment || !this.trackingForm.carrier || !this.trackingForm.trackingNumber) return;
        this.api.put<any>(`/api/shipments/${shipment.id}/tracking`, {
            carrier: this.trackingForm.carrier,
            trackingNumber: this.trackingForm.trackingNumber
        }).subscribe({
            next: (res) => {
                this.activeShipment.set(res.data ?? res);
                this.modalSuccess.set('Tracking details assigned!');
                setTimeout(() => this.modalSuccess.set(null), 2000);
            },
            error: () => this.modalError.set('Failed to assign tracking.')
        });
    }

    getShipmentStatusClass(status: string): string {
        const map: Record<string, string> = {
            PROCESSING: 'status-processing',
            DISPATCHED: 'status-shipped',
            IN_TRANSIT: 'status-paid',
            OUT_FOR_DELIVERY: 'status-pending',
            DELIVERED: 'status-delivered',
            RETURNED: 'status-cancelled'
        };
        return map[status] ?? '';
    }

    filteredUsers = computed(() => {
        const q = this.userSearch().toLowerCase();
        if (!q) return this.users();
        return this.users().filter(u =>
            u.username.toLowerCase().includes(q) ||
            u.name.toLowerCase().includes(q) ||
            u.email.toLowerCase().includes(q)
        );
    });

    filteredReviews = computed(() => {
        const f = this.reviewFilter();
        if (f === 'ALL') return this.reviews();
        return this.reviews().filter(r => r.status === f);
    });

    filteredTickets = computed(() => {
        const f = this.ticketFilter();
        if (f === 'ALL') return this.tickets();
        return this.tickets().filter(t => t.status === f);
    });

    filteredNotifications = computed(() => {
        const f = this.notifEventFilter();
        if (f === 'ALL') return this.notifications();
        return this.notifications().filter(n => n.event?.startsWith(f));
    });

    // Overview stats
    pendingReviews = computed(() => this.reviews().filter(r => r.status === 'PENDING').length);
    openTickets = computed(() => this.tickets().filter(t => t.status === 'OPEN').length);
    totalUsers = computed(() => this.users().length);
    sellers = computed(() => this.users().filter(u => u.roles.some(r => r.includes('SELLER'))));
    supportAgents = computed(() => this.users().filter(u => u.roles.some(r => r.includes('SUPPORT'))));
    customers = computed(() => this.users().filter(u => u.roles.some(r => r.includes('CUSTOMER'))));
    pendingOrders = computed(()=>this.orders().filter(o => o.status=='PROCESSING').length);

    ngOnInit(): void {
        this.loadAdminDetails();
        this.loadUsers();
        this.loadOrders();
        this.loadReviews();
        this.loadTickets();
        this.loadNotifications();
        this.loadSupportUsersForAssign();
    }

    // ── Data loading ───────────────────────────────────
    private loadAdminDetails(): void {
        const id = this.adminId();
        if (!id) return;
        this.authService.getUserDetails(id).subscribe({
            next: (res) => this.userDetails.set(res.user),
            error: () => { }
        });
    }

    loadUsers(): void {
        this.loadingUsers.set(true);
        this.api.get<any>('/auth/users').subscribe({
            next: (res) => {
                // Response is List<UserDetailsResponseDTO>
                const users = Array.isArray(res)
                    ? res.map((r: any) => r.user ?? r)
                    : [];
                this.users.set(users);
                this.loadingUsers.set(false);
            },
            error: () => this.loadingUsers.set(false)
        });
    }

    loadOrders(page = 0): void {
        this.loadingOrders.set(true);
        this.api.get<ApiPageResponse<OrderResponse>>(
            `/api/orders/admin/all?page=${page}&size=15`
        ).subscribe({
            next: (res) => {
                this.orders.set(res.data?.content ?? []);
                this.orderTotalPages.set(res.data?.totalPages ?? 0);
                this.orderTotal.set(res.data?.totalElements ?? 0);
                this.orderPage.set(page);
                this.loadingOrders.set(false);
            },
            error: () => this.loadingOrders.set(false)
        });
    }

    loadReviews(): void {
        this.loadingReviews.set(true);
        this.api.get<ReviewResponse[]>('/reviews').subscribe({
            next: (data) => {
                this.reviews.set(data);
                this.loadingReviews.set(false);
            },
            error: () => this.loadingReviews.set(false)
        });
    }

    loadTickets(): void {
        this.loadingTickets.set(true);
        this.api.get<TicketResponse[]>('/tickets').subscribe({
            next: (data) => {
                this.tickets.set(data);
                this.loadingTickets.set(false);
            },
            error: () => this.loadingTickets.set(false)
        });
    }

    loadNotifications(): void {
        this.loadingNotifications.set(true);
        this.api.get<NotificationLogResponse[]>('/notification-logs').subscribe({
            next: (data) => {
                this.notifications.set(data);
                this.loadingNotifications.set(false);
            },
            error: () => this.loadingNotifications.set(false)
        });
    }

    loadSupportUsersForAssign(): void {
        console.log('[Admin] Loading support users for assign dropdown...');
        this.authService.getSupportUsers().subscribe({
            next: (res) => {
                console.log('[Admin] getSupportUsers raw response:', res);
                const users = res.map((r: any) => r.user ?? r).filter(Boolean);
                console.log('[Admin] Mapped support users:', users);
                this.supportUsersForAssign.set(users);
            },
            error: (err) => {
                console.error('[Admin] Failed to load support users:', err);
            }
        });
    }

    // ── User management ────────────────────────────────
    onboardSeller(): void {
        if (!this.onboardUsername()) return;
        this.modalLoading.set(true);
        this.modalError.set(null);
        this.api.post<any>('/admin/onboardseller', { username: this.onboardUsername() }).subscribe({
            next: (res) => {
                this.modalLoading.set(false);
                this.modalSuccess.set(`${this.onboardUsername()} onboarded as Seller!`);
                this.loadUsers();
                setTimeout(() => {
                    this.showOnboardSellerModal.set(false);
                    this.onboardUsername.set('');
                    this.modalSuccess.set(null);
                }, 1500);
            },
            error: () => {
                this.modalLoading.set(false);
                this.modalError.set('Failed to onboard seller. Check username.');
            }
        });
    }

    onboardSupport(): void {
        if (!this.onboardUsername()) return;
        this.modalLoading.set(true);
        this.modalError.set(null);
        this.api.post<any>('/admin/onboardsupport', { username: this.onboardUsername() }).subscribe({
            next: () => {
                this.modalLoading.set(false);
                this.modalSuccess.set(`${this.onboardUsername()} onboarded as Support!`);
                this.loadUsers();
                setTimeout(() => {
                    this.showOnboardSupportModal.set(false);
                    this.onboardUsername.set('');
                    this.modalSuccess.set(null);
                }, 1500);
            },
            error: () => {
                this.modalLoading.set(false);
                this.modalError.set('Failed to onboard support. Check username.');
            }
        });
    }

    offboardUser(): void {
        const user = this.selectedUser();
        if (!user) return;
        this.modalLoading.set(true);
        this.modalError.set(null);
        this.api.post<any>('/admin/offboard', { username: user.username }).subscribe({
            next: () => {
                this.modalLoading.set(false);
                this.modalSuccess.set(`${user.username} demoted to Customer.`);
                this.loadUsers();
                setTimeout(() => {
                    this.showOffboardModal.set(false);
                    this.modalSuccess.set(null);
                }, 1500);
            },
            error: () => {
                this.modalLoading.set(false);
                this.modalError.set('Failed to offboard user.');
            }
        });
    }

    deleteUser(): void {
        const user = this.selectedUser();
        if (!user) return;
        this.modalLoading.set(true);
        this.api.delete(`/admin/deleteuser/${user.user_id}`).subscribe({
            next: () => {
                this.modalLoading.set(false);
                this.showDeleteUserModal.set(false);
                this.loadUsers();
            },
            error: () => {
                this.modalLoading.set(false);
                this.modalError.set('Failed to delete user.');
            }
        });
    }

    getUserRole(user: User): string {
        if (user.roles.some(r => r.includes('ADMIN'))) return 'ADMIN';
        if (user.roles.some(r => r.includes('SELLER'))) return 'SELLER';
        if (user.roles.some(r => r.includes('SUPPORT'))) return 'SUPPORT';
        return 'CUSTOMER';
    }

    // ── Orders ─────────────────────────────────────────

    updateOrderStatus(): void {
        const order = this.selectedOrder();
        if (!order) return;
        this.modalLoading.set(true);
        this.modalError.set(null);
        this.api.put<any>(`/api/orders/admin/${order.id}/status`, {
            status: this.newOrderStatus()
        }).subscribe({
            next: (res) => {
                this.modalLoading.set(false);
                this.modalSuccess.set('Order status updated!');
                this.loadOrders(this.orderPage());
                const updated = res.data ?? res;
                this.selectedOrder.set(updated);
                setTimeout(() => this.modalSuccess.set(null), 2000);
            },
            error: () => {
                this.modalLoading.set(false);
                this.modalError.set('Failed to update order status.');
            }
        });
    }

    prevPage(): void {
        if (this.orderPage() > 0) this.loadOrders(this.orderPage() - 1);
    }

    nextPage(): void {
        if (this.orderPage() < this.orderTotalPages() - 1) {
            this.loadOrders(this.orderPage() + 1);
        }
    }

    // ── Reviews ────────────────────────────────────────
    updateReviewStatus(reviewId: number, status: ReviewStatus): void {
        this.api.put<any>(`/reviews/${reviewId}/status`, { status }).subscribe({
            next: () => {
                this.reviews.update(rs =>
                    rs.map(r => r.id === reviewId ? { ...r, status } : r)
                );
            },
            error: () => { }
        });
    }

    deleteReview(reviewId: number): void {
        this.api.delete(`/reviews/${reviewId}`).subscribe({
            next: () => {
                this.reviews.update(rs => rs.filter(r => r.id !== reviewId));
            },
            error: () => { }
        });
    }

    // ── Tickets ────────────────────────────────────────
    openTicket(ticket: TicketResponse): void {
        this.activeTicket.set(ticket);
        this.activeMessages.set([]);
        this.replyMessage.set('');
        this.loadingMessages.set(true);
        this.selectedAssignee.set(ticket.assignedTo);
        this.assignSuccess.set(null);
        this.assignError.set(null);
        this.api.get<TicketMessageResponse[]>(`/tickets/${ticket.id}/messages`).subscribe({
            next: (data) => {
                this.activeMessages.set(data);
                this.loadingMessages.set(false);
                this.scrollToBottom();
            },
            error: () => this.loadingMessages.set(false)
        });
    }

    closeTicketView(): void {
        this.activeTicket.set(null);
        this.activeMessages.set([]);
    }

    updateTicketStatus(ticket: TicketResponse, status: TicketStatus): void {
        this.api.put<TicketResponse>(`/tickets/${ticket.id}?status=${status}`, {}).subscribe({
            next: (updated) => {
                this.tickets.update(ts => ts.map(t => t.id === ticket.id ? updated : t));
                if (this.activeTicket()?.id === ticket.id) this.activeTicket.set(updated);
            },
            error: () => { }
        });
    }

    deleteTicket(ticketId: number): void {
        this.api.delete(`/tickets/${ticketId}`).subscribe({
            next: () => {
                this.tickets.update(ts => ts.filter(t => t.id !== ticketId));
                if (this.activeTicket()?.id === ticketId) this.closeTicketView();
            },
            error: () => { }
        });
    }

    sendReply(): void {
        const ticket = this.activeTicket();
        const message = this.replyMessage().trim();
        if (!ticket || !message) return;
        this.sendingReply.set(true);
        this.api.post<TicketMessageResponse>(
            `/tickets/${ticket.id}/messages`, { message }
        ).subscribe({
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

    // ── Message helpers ────────────────────────────────
    isStaffMsg(msg: TicketMessageResponse): boolean {
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

    // ── Navigation ─────────────────────────────────────
    setSection(section: Section): void {
        this.activeSection.set(section);
        this.activeTicket.set(null);
    }

    toggleSidebar(): void {
        this.sidebarOpen.update(v => !v);
    }

    logout(): void {
        this.authService.logout();
    }

    // ── Ticket assignment ──────────────────────────────
    assignTicket(ticket: TicketResponse): void {
        const agentId = this.selectedAssignee();
        if (!agentId) return;
        this.assigningTicket.set(true);
        this.assignError.set(null);
        this.assignSuccess.set(null);
        this.api.put<TicketResponse>(`/tickets/${ticket.id}?assignedTo=${agentId}&status=IN_PROGRESS`, {}).subscribe({
            next: (updated) => {
                this.tickets.update(ts => ts.map(t => t.id === ticket.id ? updated : t));
                this.activeTicket.set(updated);
                this.assigningTicket.set(false);
                this.assignSuccess.set('Ticket assigned successfully.');
                setTimeout(() => this.assignSuccess.set(null), 3000);
            },
            error: () => {
                this.assigningTicket.set(false);
                this.assignError.set('Failed to assign ticket.');
            }
        });
    }

    // ── Helpers ────────────────────────────────────────
    normalizeDate(dateStr: string | null | undefined): Date | null {
        if (!dateStr) return null;
        const s = (dateStr.includes('Z') || dateStr.includes('+')) ? dateStr : dateStr + '+05:30';
        const d = new Date(s);
        return isNaN(d.getTime()) ? null : d;
    }

    ticketStatusLabel(status: TicketStatus): string {
        return { OPEN: 'Open', IN_PROGRESS: 'In Progress', CLOSED: 'Closed' }[status] ?? status;
    }

    toggleNotifExpand(id: number): void {
        this.expandedNotifId.update(cur => cur === id ? null : id);
    }

    getAgentName(userId: number | null): string {
        if (!userId) return 'Unassigned';
        const agent = this.users().find(u => u.user_id === userId);
        return agent ? agent.name : `Advisor #${userId}`;
    }

    getOrderStatusClass(status: OrderStatus): string {
        const map: Record<OrderStatus, string> = {
            PENDING_PAYMENT: 'status-pending',
            PAID: 'status-paid',
            PROCESSING: 'status-processing',
            SHIPPED: 'status-shipped',
            DELIVERED: 'status-delivered',
            COMPLETED: 'status-completed',
            CANCELLED: 'status-cancelled',
            REFUNDED: 'status-refunded'
        };
        return map[status] ?? '';
    }

    getReviewStatusClass(status: ReviewStatus): string {
        return { PENDING: 'status-pending', APPROVED: 'status-approved', REJECTED: 'status-rejected' }[status];
    }

    getTicketStatusClass(status: TicketStatus): string {
        return { OPEN: 'status-open', IN_PROGRESS: 'status-progress', CLOSED: 'status-closed' }[status];
    }

    getPriorityClass(priority: TicketPriority): string {
        return { LOW: 'priority-low', MEDIUM: 'priority-medium', HIGH: 'priority-high', CRITICAL: 'priority-critical' }[priority];
    }

    getNotifStatusClass(status: NotifStatus): string {
        return { QUEUED: 'status-queued', SENT: 'status-sent', FAILED: 'status-failed' }[status];
    }

    getRoleBadgeClass(role: string): string {
        return { ADMIN: 'role-admin', SELLER: 'role-seller', SUPPORT: 'role-support', CUSTOMER: 'role-customer' }[role] ?? 'role-customer';
    }
}