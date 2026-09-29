export interface NotifGroup<T> {
  label: string;
  items: T[];
}

export const EVENT_LABELS: Record<string, string> = {
  ORDER_PLACED:        'Order Placed',
  ORDER_CONFIRMED:     'Order Confirmed',
  ORDER_SHIPPED:       'Order Shipped',
  ORDER_DELIVERED:     'Order Delivered',
  ORDER_CANCELLED:     'Order Cancelled',
  PAYMENT_SUCCESS:     'Payment Confirmed',
  PAYMENT_FAILED:      'Payment Failed',
  REVIEW_SUBMITTED:    'Review Submitted',
  REVIEW_APPROVED:     'Review Approved',
  REVIEW_REJECTED:     'Review Rejected',
  RATING_SUBMITTED:    'Rating Submitted',
  SHIPMENT_DISPATCHED: 'Shipment Dispatched',
  SHIPMENT_DELIVERED:  'Shipment Delivered',
  TICKET_CREATED:      'Support Ticket Created',
  TICKET_ASSIGNED:     'Advisor Assigned',
  TICKET_REPLY:        'New Reply',
  TICKET_RESOLVED:     'Support Ticket Resolved',
  ACCOUNT_CREATED:     'Welcome to RunWay',
};

export const NOTIF_FILTER_GROUPS = [
  { label: 'All',       value: 'ALL' },
  { label: 'Orders',    value: 'ORDER' },
  { label: 'Shipments', value: 'SHIPMENT' },
  { label: 'Payments',  value: 'PAYMENT' },
  { label: 'Reviews',   value: 'REVIEW' },
  { label: 'Tickets',   value: 'TICKET' },
  { label: 'Ratings',   value: 'RATING' },
  { label: 'Account',   value: 'ACCOUNT' },
];

export interface NotifNavTarget {
  path: string;
  fragment?: string;
}

export function getNotifRoute(event: string, payloadJson?: string): NotifNavTarget | null {
  if (!event) return null;
  let payload: Record<string, string> = {};
  if (payloadJson) {
    try { payload = JSON.parse(payloadJson); } catch { /* ignore */ }
  }
  if (event === 'TICKET_REPLY' || event === 'TICKET_ASSIGNED') {
    return { path: payload['ticketId'] ? `/support/${payload['ticketId']}` : '/support' };
  }
  if (event === 'TICKET_CREATED' || event === 'TICKET_RESOLVED') return { path: '/support' };
  if (event.startsWith('ORDER') || event.startsWith('PAYMENT') || event.startsWith('SHIPMENT')) return { path: '/orders' };
  if (event.startsWith('REVIEW') || event.startsWith('RATING')) {
    if (!payload['productId']) return null;
    return { path: `/product/${payload['productId']}`, fragment: 'reviews' };
  }
  if (event === 'ACCOUNT_CREATED') return { path: '/account' };
  return null;
}

export function getEventLabel(event: string): string {
  if (!event) return 'Notification';
  return (
    EVENT_LABELS[event] ??
    event.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())
  );
}

export function groupNotificationsByDate<T extends { createdAt: string }>(
  items: T[]
): NotifGroup<T>[] {
  const todayStart = new Date();
  todayStart.setHours(0, 0, 0, 0);
  const weekStart = new Date(todayStart.getTime() - 6 * 86_400_000);

  const toMs = (s: string) => {
    if (!s) return 0;
    const d = new Date(s.includes('Z') || s.includes('+') ? s : s + 'Z');
    return isNaN(d.getTime()) ? 0 : d.getTime();
  };

  const sorted = [...items].sort((a, b) => toMs(b.createdAt) - toMs(a.createdAt));

  const today: T[] = [];
  const thisWeek: T[] = [];
  const earlier: T[] = [];

  for (const n of sorted) {
    const t = toMs(n.createdAt);
    if (t >= todayStart.getTime()) today.push(n);
    else if (t >= weekStart.getTime()) thisWeek.push(n);
    else earlier.push(n);
  }

  return [
    { label: 'Today',     items: today },
    { label: 'This Week', items: thisWeek },
    { label: 'Earlier',   items: earlier },
  ].filter(g => g.items.length > 0);
}
