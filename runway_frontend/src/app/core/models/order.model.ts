// ── API wrapper ───────────────────────────────────────────────────────────────
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// ── Paged wrapper ─────────────────────────────────────────────────────────────
export interface PagedData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── Cart ──────────────────────────────────────────────────────────────────────
export interface AddCartItemRequest {
  productId: number;
  productTitle: string;
  unitPrice: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

export interface CartItemResponse {
  id: number;
  productId: number;
  productTitle: string;
  quantity: number;
  unitPrice: number;
}

export interface CartResponse {
  id: number;
  userId: number;
  items: CartItemResponse[];
  total: number;
  totalItems: number;
  updatedAt: string;
}

// ── Order ─────────────────────────────────────────────────────────────────────
export interface CheckoutRequest {
  shippingAddress: string;
  notes?: string;
}

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDED';

export interface OrderLineResponse {
  id: number;
  productId: number;
  productTitle: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderResponse {
  id: number;
  orderNumber: string;
  userId: number;
  status: OrderStatus;
  totalAmount: number;
  currency: string;
  shippingAddress: string;
  notes: string | null;
  lines: OrderLineResponse[];
  createdAt: string;
  updatedAt: string;
}

export type PagedOrderResponse = PagedData<OrderResponse>;

// ── Shipment ──────────────────────────────────────────────────────────────────
export type ShipmentStatus =
  | 'PROCESSING'
  | 'DISPATCHED'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'RETURNED';

export interface ShipmentResponse {
  id: number;
  orderId: number;
  carrier: string | null;
  trackingNumber: string | null;
  status: ShipmentStatus;
  shippingAddress: string;
  shippedAt: string | null;
  deliveredAt: string | null;
  createdAt: string;
  updatedAt: string;
}

// ── Payment ───────────────────────────────────────────────────────────────────
// Matches PaymentDtos.PaymentIntentResponse exactly

// POST /payment/{orderId}/intent — request body
export interface CreatePaymentIntentRequest {
  amount: number;           // required — backend throws if null
}

// PUT /payment/{orderId}/status — request body
export interface UpdatePaymentStatusRequest {
  status: PaymentStatus;    // required — backend returns 400 if missing
}

// Payment status values (string enum from PaymentIntent.status)
export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';

// PaymentIntentResponse — from PaymentDtos.PaymentIntentResponse.from(intent)
export interface PaymentIntentResponse {
  id: number;
  orderId: number;
  userId: number;
  amount: number;
  currency: string;          // e.g. "INR"
  status: PaymentStatus;
  razorpayOrderId: string | null;  // Razorpay integration — may be null
  createdAt: string;
  updatedAt: string;
}