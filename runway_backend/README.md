# RUNWAY Backend

The backend of **RUNWAY** is a Java/Spring Boot microservices platform for a full-stack e-commerce application.

It is organized as seven independently runnable services connected through **Spring Cloud Netflix Eureka** for service discovery and **Spring Cloud Gateway** for centralized routing and JWT validation.

> The root repository README contains the complete product overview. This README focuses on the backend architecture, services, configuration, and local development.

---

## Architecture

```text
                         ┌──────────────────────┐
                         │   Angular Frontend   │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │     API Gateway      │
                         │        :9191         │
                         │ Routing + JWT        │
                         │ validation           │
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │   Eureka Registry    │
                         │        :8761         │
                         └──────────┬───────────┘
                                    │
        ┌───────────────────────────┼────────────────────────────┐
        │             │             │             │              │
        ▼             ▼             ▼             ▼              ▼
   Auth :9090   Catalog :8765   Order :2000   Payment :56272   Engagement :8083
        │             │             │             │              │
        ▼             ▼             ▼             ▼              ▼
   ecom_db       catalog_db     order_db     payment_db     engagementdb
```

### Request flow

1. The frontend sends requests to the API Gateway.
2. The Gateway routes requests to the appropriate service using Eureka service discovery.
3. Protected routes require a Bearer JWT.
4. The Gateway validates the token through the Auth Service.
5. On successful validation, the Gateway injects `X-User-Id` and `X-User-Role` headers before forwarding the request.
6. Each business service owns its own database.

---

## Services

| Service | Port | Database | Responsibility |
|---|---:|---|---|
| **Service Registry** | `8761` | — | Eureka service discovery |
| **API Gateway** | `9191` | — | Routing, JWT validation, identity headers, API-doc aggregation |
| **Auth Service** | `9090` | `ecom_db` | Registration, login, JWT validation, user/admin management |
| **Catalog Service** | `8765` | `catalog_db` | Products, categories, inventory, product media, wishlist |
| **Order Service** | `2000` | `order_db` | Cart, checkout, orders, shipments |
| **Payment Service** | `56272` | `payment_db` | Payment intents and payment status |
| **Customer Engagement Service** | `8083` | `engagementdb` | Ratings, reviews, support tickets, notifications |

---

## Technology Stack

- **Java**
- **Spring Boot 3.x**
- **Spring Cloud Gateway**
- **Spring Cloud Netflix Eureka**
- **Spring Security**
- **JWT**
- **Spring Data JPA / Hibernate**
- **MySQL 8**
- **Flyway**
- **OpenAPI / Swagger**
- **OpenFeign**
- **Maven**
- **JUnit / Mockito**
- **Razorpay** for payment processing

The repository includes Maven Wrapper configuration using Maven **3.9.14**, so a separate Maven installation is not required when using the wrapper.

---

## Authentication & Authorization

RUNWAY uses JWT-based authentication.

### Roles

| Role | Main capabilities |
|---|---|
| `CUSTOMER` | Browse products, manage wishlist/cart, place orders, review products, create support tickets |
| `SELLER` | Manage owned products and inventory |
| `SUPPORT` | Handle customer support tickets, review moderation, notification-related operations |
| `ADMIN` | Platform administration and privileged management operations |

The API Gateway validates protected requests and forwards authenticated identity information using:

```text
X-User-Id
X-User-Role
```

The Auth Service also supports refresh-token handling.

---

## Service Details

### 1. Service Registry

**Port:** `8761`

Technology:

- Spring Cloud Netflix Eureka

The registry allows backend services to register themselves and discover other services dynamically.

Open:

```text
http://localhost:8761
```

---

### 2. API Gateway

**Port:** `9191`

The gateway is the main backend entry point.

Responsibilities:

- Request routing
- JWT validation for protected routes
- Identity header injection
- Eureka-based service discovery
- Aggregated OpenAPI documentation

Main route groups include:

```text
/auth/**
/admin/**
/catalog/**
/api/cart/**
/api/orders/**
/api/shipments/**
/payment/**
/products/**
/reviews/**
/ratings/**
/tickets/**
/notification-templates/**
/notification-logs/**
```

Swagger UI:

```text
http://localhost:9191/swagger-ui/index.html
```

---

### 3. Auth Service

**Port:** `9090`

Database:

```text
ecom_db
```

Responsibilities:

- Customer registration
- Login
- JWT access-token generation
- Refresh-token handling
- Token validation
- User details
- Account deletion
- Admin user-management operations
- Seller/support onboarding

Typical public endpoints:

```text
POST /auth/login
POST /auth/signup
POST /auth/refreshtoken
POST /auth/validate
```

Admin operations are exposed under:

```text
/admin/**
```

---

### 4. Catalog Service

**Port:** `8765`

Database:

```text
catalog_db
```

Responsibilities:

- Product management
- Category management
- Inventory management
- Product image/media handling
- Seller-specific product operations
- Wishlist functionality

Representative routes:

```text
/catalog/products
/catalog/categories
/catalog/inventory/**
/catalog/wishlist/**
```

Product creation supports multipart requests containing product data and images.

Catalog images are served from the configured local upload directory.

---

### 5. Order Service

**Port:** `2000`

Database:

```text
order_db
```

Responsibilities:

- Shopping cart
- Cart item management
- Checkout
- Order creation
- Order lifecycle
- Shipment tracking
- Administrative order management

Representative routes:

```text
/api/cart/**
/api/orders/**
/api/shipments/**
```

Protected cart and order requests use the authenticated user's identity supplied by the gateway.

---

### 6. Payment Service

**Port:** `56272`

Database:

```text
payment_db
```

Responsibilities:

- Payment intent creation
- Payment status management
- Payment audit information
- Razorpay integration

Representative route:

```text
/payment/**
```

Razorpay credentials are supplied through environment variables and are **not stored in source control**.

---

### 7. Customer Engagement Service

**Port:** `8083`

Database:

```text
engagementdb
```

Responsibilities:

- Product ratings
- Product reviews
- Review moderation
- Customer support tickets
- Ticket messages
- Notification templates
- Notification logs

Representative route groups:

```text
/products/**
/reviews/**
/ratings/**
/tickets/**
/notification-templates/**
/notification-logs/**
```

Database schema changes are managed with Flyway migrations.

---

## Database Setup

Create the databases before starting the services:

```sql
CREATE DATABASE ecom_db;
CREATE DATABASE catalog_db;
CREATE DATABASE engagementdb;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
```

The services use separate databases to maintain service-level data ownership.

---

## Environment Variables

The backend expects configuration through environment variables rather than hard-coded credentials.

Required variables:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET

RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET

CATALOG_UPLOAD_DIR
CATALOG_IMAGE_BASE_URL

ADMIN_USERNAME
ADMIN_EMAIL
ADMIN_PASSWORD
```

### Example

Do **not** commit real credentials.

For local development, configure the variables in your operating-system environment or another local secret-management mechanism.

Example placeholders:

```text
DB_USERNAME=<your-mysql-username>
DB_PASSWORD=<your-mysql-password>
JWT_SECRET=<long-random-secret>

RAZORPAY_KEY_ID=<razorpay-test-key>
RAZORPAY_KEY_SECRET=<razorpay-test-secret>

CATALOG_UPLOAD_DIR=<local-upload-directory>
CATALOG_IMAGE_BASE_URL=http://localhost:8765/api/v1/catalog/images/

ADMIN_USERNAME=<admin-username>
ADMIN_EMAIL=<admin-email>
ADMIN_PASSWORD=<admin-password>
```

---

## Prerequisites

Install:

- **JDK 21+**
- **MySQL 8.x**
- Internet access for Maven dependency resolution

Maven Wrapper files are included in the services.

---

## Running the Backend

Open a separate terminal for each service.

### 1. Start Service Registry

```powershell
cd service-registry
.\mvnw.cmd spring-boot:run
```

### 2. Start Auth Service

```powershell
cd auth-service
.\mvnw.cmd spring-boot:run
```

### 3. Start Catalog Service

```powershell
cd catalog-service
.\mvnw.cmd spring-boot:run
```

### 4. Start Order Service

```powershell
cd order-service
.\mvnw.cmd spring-boot:run
```

### 5. Start Customer Engagement Service

```powershell
cd customer-engagement-service
.\mvnw.cmd spring-boot:run
```

### 6. Start Payment Service

```powershell
cd payment-service
.\mvnw.cmd spring-boot:run
```

### 7. Start API Gateway

```powershell
cd api-gateway
.\mvnw.cmd spring-boot:run
```

The services should be started with the Eureka Registry available so that they can register and discover each other.

---

## Verification

### Eureka

```text
http://localhost:8761
```

### Aggregated Swagger

```text
http://localhost:9191/swagger-ui/index.html
```

### Gateway OpenAPI

```text
http://localhost:9191/v3/api-docs
```

The aggregated Swagger UI is the preferred place to explore the backend APIs.

---

## Testing

Each service contains its own test suite.

Run all tests for an individual service:

```powershell
.\mvnw.cmd test
```

Run a particular service from its directory:

```powershell
cd auth-service
.\mvnw.cmd test
```

The project was verified locally with the backend test suites passing after the current configuration and test fixes.

---

## Backend Project Structure

```text
runway_backend/
│
├── api-gateway/
│   └── src/
│
├── auth-service/
│   └── src/
│
├── catalog-service/
│   └── src/
│
├── customer-engagement-service/
│   └── src/
│
├── order-service/
│   └── src/
│
├── payment-service/
│   └── src/
│
└── service-registry/
    └── src/
```

Each service is independently buildable and runnable.

---

## API Documentation

The API Gateway provides the central Swagger UI:

```text
http://localhost:9191/swagger-ui/index.html
```

Individual services also expose their OpenAPI documentation according to their Springdoc configuration.

Use the Swagger documentation for the current request/response schemas and endpoint details rather than relying on duplicated endpoint lists in this README.

---

## Development Notes

### Service discovery

Business services register with Eureka and are addressed by logical service names through the gateway.

Examples:

```text
lb://AUTH-SERVICE
lb://CATALOG-SERVICE
lb://ORDER-SERVICE
lb://PAYMENT-SERVICE
lb://CUSTOMER-ENGAGEMENT-SERVICE
```

### Security

Protected requests should contain:

```http
Authorization: Bearer <JWT>
```

The gateway validates the token and forwards authenticated identity headers to downstream services.

### Configuration

Database credentials, JWT secrets, Razorpay credentials, admin credentials, and local catalog storage configuration are externalized through environment variables.

No production or development secret values should be committed to Git.

---

## Related Documentation

For the complete application overview, frontend setup, feature walkthrough, and full-stack architecture, see the repository root:

```text
../README.md
```

---

## License

No explicit open-source license has been assigned to this repository at this time.
