# Customer Engagement Service

Customer Engagement Service manages **Ratings, Reviews, Support Tickets, Ticket Conversations, and Notifications** for the Online Shopping Platform.

All APIs are exposed **via API Gateway**.  
Authentication is handled at the **gateway**, while this service enforces **authorization and business rules**.

---

## Base URL

```

<http://localhost:9191>

```

Swagger:
```

/service/customer-engagement-service/v3/api-docs

```

---

## Security Model

| Aspect | Details |
|-----|--------|
| Authentication | API Gateway (JWT validation) |
| Authorization | Service‑level (`@PreAuthorize`, ownership checks) |
| Identity Headers | `X-User-Id`, `X-User-Role` |
| Roles | CUSTOMER, ADMIN, SUPPORT |

---

# Ratings APIs

**Business Rules**
- One rating per user per product
- Owner‑scoped update/delete
- Admin override allowed

| Method | Endpoint | Description | Roles |
|------|--------|-------------|------|
| GET | `/products/{productId}/ratings` | Get ratings for a product. **Query params:** `mine=true` → return only logged‑in user’s rating for that product | Public (`mine=true` → Auth) |
| POST | `/products/{productId}/ratings` | Create a new rating for a product | CUSTOMER, ADMIN |
| PUT | `/products/{productId}/ratings` | Update existing rating for the product | CUSTOMER (owner), ADMIN |
| DELETE | `/products/{productId}/ratings` | Delete rating for the product | CUSTOMER (owner), ADMIN |
| GET | `/products/{productId}/ratings/average` | Get average rating for a product | Public |
| GET | `/ratings` | Get ratings. **Query params:** `mine=true` → user’s ratings, `mine=false` → all ratings (admin) | CUSTOMER, ADMIN |

---

# Reviews APIs

**Business Rules**
- One review per user per product
- Purchase required before review
- Moderation workflow
- Public sees only APPROVED reviews

| Method | Endpoint | Description | Roles |
|------|--------|-------------|------|
| GET | `/products/{productId}/reviews` | Get product reviews. **Query params:** `mine=true` → logged‑in user’s reviews for that product, `mine=false` → approved reviews | Public (`mine=true` → Auth) |
| POST | `/products/{productId}/reviews` | Create product review (initial status = `PENDING`) | CUSTOMER, ADMIN |
| GET | `/reviews` | Get reviews. **Query params:** `mine=true` → user’s reviews, `mine=false` → all reviews | CUSTOMER (own), ADMIN |
| GET | `/reviews/{id}` | Get review by ID | ADMIN |
| PUT | `/reviews/{id}` | Update review content. Customer edits reset status to `PENDING` | CUSTOMER (owner), ADMIN |
| PUT | `/reviews/{id}/status` | Approve or reject a review (only `PENDING`) | ADMIN |
| DELETE | `/reviews/{id}` | Delete review | CUSTOMER (owner), ADMIN |

---

# Support Tickets APIs

**Ticket Lifecycle**
```

OPEN → IN_PROGRESS → CLOSED

```

**Rules**
- CUSTOMER → own tickets only
- ADMIN / SUPPORT → all tickets
- Only ADMIN / SUPPORT can update ticket status
- Only ADMIN can delete tickets

| Method | Endpoint | Description | Roles |
|------|--------|-------------|------|
| POST | `/tickets` | Create a support ticket (initial status = `OPEN`) | CUSTOMER, ADMIN |
| GET | `/tickets` | Get tickets. **Query params:** `mine=true` → own tickets, `status` → filter by status | CUSTOMER, ADMIN, SUPPORT |
| GET | `/tickets/{id}` | Get ticket by ID (ownership enforced) | CUSTOMER (owner), ADMIN, SUPPORT |
| PUT | `/tickets/{id}` | Update ticket. **Query params:** `status=OPEN|IN_PROGRESS|CLOSED` | ADMIN, SUPPORT |
| DELETE | `/tickets/{id}` | Delete ticket permanently | ADMIN |

---

# Ticket Messages APIs

**Rules**
- User must have access to the ticket
- Sender role is derived automatically

| Method | Endpoint | Description | Roles |
|------|--------|-------------|------|
| GET | `/tickets/{ticketId}/messages` | Get messages for a ticket | CUSTOMER (owner), ADMIN, SUPPORT |
| POST | `/tickets/{ticketId}/messages` | Send message to ticket conversation | CUSTOMER, ADMIN, SUPPORT |

**Sender Role Mapping**
- CUSTOMER → CUSTOMER  
- ADMIN / SUPPORT → SUPPORT  

---

# Notification Logs APIs

| Method | Endpoint | Description | Roles |
|------|--------|-------------|------|
| POST | `/notification-logs` | Trigger a notification event manually | ADMIN, SUPPORT |
| GET | `/notification-logs` | Get notification logs. **Query params:** `mine=true` → user’s logs, `status` → filter by status | CUSTOMER (own), ADMIN, SUPPORT |
| GET | `/notification-logs/{id}` | Get notification log by ID | ADMIN, SUPPORT |

**Supported Events**
- ORDER_PLACED, ORDER_CANCELLED, ORDER_DELIVERED
- REVIEW_SUBMITTED, REVIEW_APPROVED
- SHIPMENT_DISPATCHED, SHIPMENT_DELIVERED
- TICKET_CREATED, TICKET_RESOLVED
- RATING_SUBMITTED

---

# Notification Templates APIs (Admin Only)

| Endpoint | Description | Roles |
|--------|-------------|------|
| `/notification-templates/**` | Create, update, delete notification templates | ADMIN |

- Hidden from Swagger UI
- Internal admin functionality

---



