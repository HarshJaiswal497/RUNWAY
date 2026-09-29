# Online Shopping Application — Microservices Platform

**Version:** 1.0  
**API Gateway Base URL:** `http://localhost:9191`  
**Swagger UI (Aggregated):** `http://localhost:9191/swagger-ui.html`

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Microservices Summary](#microservices-summary)
- [Roles](#roles)
- [Auth Service](#auth-service)
- [Catalog Service](#catalog-service)
- [Order Service](#order-service)
- [Payment Service](#payment-service)
- [Customer Engagement Service](#customer-engagement-service)
- [Service Registry](#service-registry)
- [API Gateway](#api-gateway)
- [Complete API Endpoint Reference](#complete-api-endpoint-reference)

---

## Architecture Overview

```
┌────────────┐      ┌──────────────┐       ┌──────────────────┐
│  Frontend  │────▶│  API Gateway  │──────▶│  Service Registry │
│  (Client)  │      │  :9191       │       │  (Eureka) :8761   │
└────────────┘      └──────┬───────┘       └──────────────────┘
                            │
          ┌─────────────────┼─────────────────────────┐
          │                 │                           │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌───────────────▼──────────┐
   │ Auth Service │  │  Catalog    │  │  Customer Engagement     │
   │ :9090        │  │  Service    │  │  Service :8083            │
   └─────────────┘  │  :8082      │  └──────────────────────────┘
                     └─────────────┘
          ┌──────────────┐     ┌──────────────┐
          │ Order Service │     │ Payment      │
          │ :8081         │     │ Service :8084│
          └──────────────┘     └──────────────┘
```

All inter-service communication is routed through the **API Gateway**, which validates JWT tokens and injects `X-User-Id` and `X-User-Role` headers into downstream requests.

---

## Microservices Summary

| Service | Port | Description |
|---|---|---|
| **Service Registry** | 8761 | Eureka-based service discovery server. All microservices register here. |
| **API Gateway** | 9191 | Central entry point. Routes requests, validates JWT tokens, injects identity headers. |
| **Auth Service** | 9090 | Handles user registration, login, JWT issuance/validation, and admin user management. |
| **Catalog Service** | 8082 | Manages products, categories, and inventory. |
| **Order Service** | 8081 | Manages shopping carts, order placement, order lifecycle, and shipment tracking. |
| **Payment Service** | 8084 | Handles payment intent creation and payment status updates. |
| **Customer Engagement Service** | 8083 | Manages ratings, reviews, support tickets, notifications. |

---

## Roles

| Role | Rank | Description |
|---|---|---|
| `ADMIN` | 4 | Full platform access. Can onboard/offboard users, manage all orders, products, and settings. |
| `SELLER` | 3 | Can create and manage their own products and inventory. |
| `SUPPORT` | 2 | Can manage customer support tickets, moderate reviews, and handle notifications. |
| `CUSTOMER` | 1 | Default role on sign-up. Can browse products, manage cart, place orders, and submit reviews/ratings. |

---

## Auth Service

**Base URL:** `http://localhost:9090`  
**Gateway Prefix:** `/auth/**`, `/admin/**`  
**Database:** MySQL (`ecom_db`)  
**Authentication:** JWT (access token + refresh token via cookie)

### Authentication Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/auth/login` | ❌ | Any | Authenticate user, returns JWT access & refresh tokens |
| `POST` | `/auth/signup` | ❌ | Any | Register a new customer account |
| `POST` | `/auth/refreshtoken` | 🔒 Cookie | Any | Refresh expired access token using refresh token |
| `POST` | `/auth/validate` | ❌ | Any | Validate a JWT token and return user details |
| `GET` | `/auth/userdetails?userId={id}` | 🔒 Bearer | Any | Get user details by user ID |
| `DELETE` | `/auth/deleteaccount` | 🔒 Bearer | Any | Delete own account |

### Admin Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/admin/onboardseller` | 🔒 Bearer + `X-User-Role` | `ADMIN` | Promote a user to Seller role |
| `POST` | `/admin/onboardsupport` | 🔒 Bearer + `X-User-Role` | `ADMIN` | Promote a user to Support role |
| `POST` | `/admin/offboard` | 🔒 Bearer + `X-User-Role` | `ADMIN` | Demote a user back to Customer role |
| `DELETE` | `/admin/deleteuser/{userId}` | 🔒 Bearer + `X-User-Role` | `ADMIN` | Delete any user by ID |

---

## Catalog Service

**Base URL:** `http://localhost:8082`  
**Gateway Prefix:** `/catalog/**`

### Product Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/catalog/products` | 🔒 | `SELLER`, `ADMIN` | Create a new product (multipart: product JSON + images) |
| `GET` | `/catalog/products` | ❌ | Any | List all products |
| `GET` | `/catalog/products/{id}` | ❌ | Any | Get product by ID |
| `GET` | `/catalog/products/category/{categoryId}` | ❌ | Any | Get products by category |
| `GET` | `/catalog/products/seller/{sellerId}` | ❌ | Any | Get products by seller |
| `PUT` | `/catalog/products/{id}` | 🔒 | `SELLER` (own), `ADMIN` | Update a product |
| `DELETE` | `/catalog/products/{id}` | 🔒 | `SELLER` (own), `ADMIN` | Delete a product |

### Category Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/catalog/categories` | 🔒 | `ADMIN` | Create a new category |
| `GET` | `/catalog/categories` | ❌ | Any | Get all root categories |

### Inventory Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `GET` | `/catalog/inventory/{productId}` | ❌ | Any | Check stock for a product |
| `PATCH` | `/catalog/inventory/{productId}/update?amount={n}` | 🔒 | `SELLER` (own), `ADMIN` | Update stock quantity |

---

## Order Service

**Base URL:** `http://localhost:8081`  
**Gateway Prefix:** `/api/cart/**`, `/api/orders/**`, `/api/shipments/**`

### Cart Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `GET` | `/api/cart/{userId}` | 🔒 | Own user / `ADMIN` | Get cart contents |
| `POST` | `/api/cart/{userId}/items` | 🔒 | Own user / `ADMIN` | Add item to cart |
| `PUT` | `/api/cart/{userId}/items/{productId}` | 🔒 | Own user / `ADMIN` | Update item quantity |
| `DELETE` | `/api/cart/{userId}/items/{productId}` | 🔒 | Own user / `ADMIN` | Remove item from cart |
| `DELETE` | `/api/cart/{userId}` | 🔒 | Own user / `ADMIN` | Clear entire cart |

### Order Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/api/orders/{userId}` | 🔒 | Own user / `ADMIN` | Place order (checkout cart) |
| `GET` | `/api/orders/{userId}` | 🔒 | Own user / `ADMIN` | Get my orders (paginated) |
| `GET` | `/api/orders/{userId}/{id}` | 🔒 | Own user / `ADMIN` | Get specific order by ID |
| `PUT` | `/api/orders/{userId}/{id}/cancel` | 🔒 | Own user / `ADMIN` | Cancel order (PENDING_PAYMENT only) |
| `GET` | `/api/orders/admin/all` | 🔒 | `ADMIN` | Get all orders (paginated) |
| `PUT` | `/api/orders/admin/{id}/status` | 🔒 | `ADMIN` | Update order status |
| `GET` | `/api/orders/exists?userId=&productId=&status=` | 🔒 | Internal | Check if user purchased a product |

### Shipment Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `GET` | `/api/shipments/order/{orderId}` | 🔒 | Any authenticated | Track shipment by order ID |
| `PUT` | `/api/shipments/{id}/status` | 🔒 | `ADMIN` | Update shipment status |
| `PUT` | `/api/shipments/{id}/tracking` | 🔒 | `ADMIN` | Assign carrier and tracking number |

---

## Payment Service

**Base URL:** `http://localhost:8084`  
**Gateway Prefix:** `/payment/**`

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/payment/{orderId}/intent` | 🔒 `X-User-Id` | Any authenticated | Create a payment intent for an order |
| `PUT` | `/payment/{orderId}/status` | 🔒 `X-User-Id` + `X-User-Role` | Any authenticated | Update payment status (e.g., COMPLETED, FAILED) |

---

## Customer Engagement Service

**Base URL:** `http://localhost:8083`  
**Gateway Prefix:** `/product/**`, `/ratings/**`, `/tickets/**`, `/notification-templates/**`, `/notification-logs/**`  
**Swagger UI:** `http://localhost:8083/swagger-ui.html`

### Ratings Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `GET` | `/products/{productId}/rating` | 🔒 | `CUSTOMER` | Get my rating for a product |
| `PUT` | `/products/{productId}/rating` | 🔒 | `CUSTOMER` | Create or update my rating |
| `DELETE` | `/products/{productId}/rating` | 🔒 | `CUSTOMER` | Delete my rating |
| `GET` | `/ratings?mine={true\|false}` | 🔒 | Any authenticated | Get all ratings (optionally only mine) |
| `GET` | `/products/{productId}/ratings` | ❌ | Any | Get all ratings for a product |
| `GET` | `/products/{productId}/ratings/average` | ❌ | Any | Get average rating for a product |

### Reviews Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/product/reviews` | 🔒 | `CUSTOMER` | Create a review (starts as PENDING) |
| `PUT` | `/product/reviews/{id}` | 🔒 | Owner / `ADMIN` | Update a review |
| `DELETE` | `/product/reviews/{id}` | 🔒 | Owner / `ADMIN` | Delete a review |
| `GET` | `/product/reviews` | 🔒 | Any authenticated | Get all approved reviews |
| `GET` | `/product/reviews/{productId}` | ❌ | Any | Get reviews by product |
| `GET` | `/product/reviews/user` | 🔒 | `CUSTOMER` | Get my reviews |
| `GET` | `/product/reviews/id/{id}` | 🔒 | Any authenticated | Get review by ID |
| `PUT` | `/product/reviews/{id}/approve` | 🔒 | `SUPPORT`, `ADMIN` | Approve a review |
| `PUT` | `/product/reviews/{id}/reject` | 🔒 | `SUPPORT`, `ADMIN` | Reject a review |

### Tickets Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/tickets` | 🔒 | `CUSTOMER` | Create a support ticket |
| `GET` | `/tickets/me` | 🔒 | `CUSTOMER` | Get my tickets |
| `GET` | `/tickets` | 🔒 | `SUPPORT`, `ADMIN` | Get all tickets |
| `PUT` | `/tickets/{ticketId}/assign` | 🔒 | `SUPPORT`, `ADMIN` | Assign a ticket |
| `PUT` | `/tickets/{ticketId}/close` | 🔒 | `SUPPORT`, `ADMIN` | Close a ticket |

### Ticket Messages Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `GET` | `/tickets/{ticketId}/messages` | 🔒 | Ticket participants | Get messages for a ticket |
| `POST` | `/tickets/{ticketId}/messages` | 🔒 | Ticket participants | Add a message to a ticket |

### Notification Templates Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/notification-templates` | 🔒 | `ADMIN` | Create a notification template |
| `GET` | `/notification-templates/{id}` | 🔒 | `SUPPORT`, `ADMIN` | Get template by ID |
| `PUT` | `/notification-templates/{id}` | 🔒 | `ADMIN` | Update a template |
| `DELETE` | `/notification-templates/{id}` | 🔒 | `ADMIN` | Delete a template |
| `GET` | `/notification-templates?name=&channel=` | 🔒 | `SUPPORT`, `ADMIN` | Query templates |

### Notification Logs Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|---|---|---|---|---|
| `POST` | `/notification-logs` | 🔒 | Internal / `ADMIN` | Create a notification log |
| `GET` | `/notification-logs/{id}` | 🔒 | `SUPPORT`, `ADMIN` | Get log by ID |
| `GET` | `/notification-logs/me` | 🔒 | Any authenticated | Get my notification logs |
| `GET` | `/notification-logs/me/status/{status}` | 🔒 | Any authenticated | Get my logs by status (QUEUED, SENT, FAILED) |
| `GET` | `/notification-logs/status/{status}` | 🔒 | `SUPPORT`, `ADMIN` | Get all logs by status |

---

## Service Registry

**URL:** `http://localhost:8761`  
**Technology:** Spring Cloud Netflix Eureka

The Service Registry provides service discovery. All microservices register themselves on startup, and the API Gateway uses it to load-balance requests via `lb://SERVICE-NAME` URIs.

---

## API Gateway

**URL:** `http://localhost:9191`  
**Technology:** Spring Cloud Gateway

### Responsibilities

1. **Routing** — Maps incoming requests to the correct downstream microservice.
2. **Authentication** — Validates JWT tokens via the Auth Service `/auth/validate` endpoint.
3. **Header Injection** — Adds `X-User-Id`, `X-User-Role`, and `X-Username` headers to authenticated requests.
4. **Swagger Aggregation** — Exposes a unified Swagger UI at `/swagger-ui.html` that aggregates API docs from all services.

### Route Mapping

| Route Pattern | Target Service |
|---|---|
| `/auth/**`, `/admin/**` | Auth Service |
| `/catalog/products/**` | Catalog Service |
| `/catalog/inventory/**` | Catalog Service |
| `/catalog/categories/**` | Catalog Service |
| `/api/cart/**`, `/api/orders/**`, `/api/shipments/**` | Order Service |
| `/payment/**` | Payment Service |
| `/product/**`, `/ratings/**`, `/tickets/**`, `/notification-templates/**`, `/notification-logs/**` | Customer Engagement Service |

---

## Tech Stack

| Technology                  | Purpose |
|-----------------------------|---|
| Java 21+                    | Primary language |
| Spring Boot 3.x             | Application framework |
| Spring Cloud Gateway        | API Gateway |
| Spring Cloud Netflix Eureka | Service discovery |
| Spring Security + JWT       | Authentication & authorization |
| Spring Data JPA             | ORM / data access |
| MySQL                       | Primary database (Auth Service) |
| H2                          | Development database (Customer Engagement Service) |
| Flyway                      | Database migrations (Customer Engagement Service) |
| Swagger / OpenAPI 3         | API documentation |
| Lombok                      | Boilerplate reduction |
| Feign Client                | Inter-service communication |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 4.0.0

### Startup Order

1. **Service Registry** — `cd service-registry && mvn spring-boot:run`
2. **Auth Service** — `cd auth-service && mvn spring-boot:run`
3. **Catalog Service** — `cd catalog-service && mvn spring-boot:run`
4. **Order Service** — `cd order-service && mvn spring-boot:run`
5. **Payment Service** — `cd payment-service && mvn spring-boot:run`
6. **Customer Engagement Service** — `cd customer-engagement-service && mvn spring-boot:run`
7. **API Gateway** — `cd api-gateway && mvn spring-boot:run`

### Verify

- Eureka Dashboard: [http://localhost:8761](http://localhost:8761)
- Swagger UI: [http://localhost:9191/swagger-ui.html](http://localhost:9191/swagger-ui.html)

---


