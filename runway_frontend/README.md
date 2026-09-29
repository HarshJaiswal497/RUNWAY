# RUNWAY Frontend

**RUNWAY** is the Angular frontend for a full-stack e-commerce application powered by Spring Boot microservices. It provides product discovery, shopping and checkout flows, account management, and role-specific interfaces for customers, sellers, administrators, and support staff.

> For the complete application architecture and backend setup, see the [repository README](../README.md) and [backend documentation](../runway_backend/README.md).

## Technology Stack

- **Angular 21.2.x** and Angular CLI 21.2.8
- **TypeScript 5.9**
- **Angular Router** with lazy-loaded feature components
- **Angular HttpClient** and RxJS
- **Tailwind CSS 4** and component styles
- **Vitest** for unit tests
- **npm** for dependency management

## Features

### Shopping experience

- Home page and category browsing
- Product search and product details
- Product images and category collections
- Wishlist for signed-in customers
- Shopping cart and checkout
- Order history, order details, and shipment tracking
- Customer account management

### Authentication and access control

- Customer sign-up and login
- JWT-based API requests via an HTTP interceptor
- Route guards for authenticated users and role-restricted pages
- Dedicated seller, admin, and support dashboards

### Additional pages and tools

- Customer notifications
- Customer support interfaces
- Information, brand story, and lookbook pages

The available features depend on the logged-in user's role and the backend services being available.

## Project Structure

```text
runway_frontend/
├── public/                   # Static assets
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── guards/       # Authentication and role guards
│   │   │   ├── interceptors/ # JWT HTTP interceptor
│   │   │   ├── models/       # Shared data models
│   │   │   ├── services/     # API and domain services
│   │   │   └── utils/        # Shared utilities
│   │   ├── features/         # Route-level feature components
│   │   ├── app.config.ts     # Application providers
│   │   └── app.routes.ts     # Application routes
│   └── ...
├── angular.json
├── package.json
└── package-lock.json
```

### Core services

The frontend includes services for authentication, catalog data, cart, orders, payments, product images, ratings, reviews, shipments, notifications, and customer support.

## Prerequisites

- **Node.js** and **npm** compatible with Angular 21
- The RUNWAY backend running locally for API-dependent features
- A running MySQL-backed microservices environment for full end-to-end workflows

## Run Locally

From the repository root, open PowerShell:

```powershell
cd D:\runway\runway_frontend
npm install
npm start
```

Or start the Angular development server directly:

```powershell
npx ng serve
```

Open **http://localhost:4200/**. Angular reloads the application when source files change.

> Start the backend services before testing login, product management, cart, checkout, payments, or other API-dependent workflows.

## Backend Integration

The frontend communicates with the **RUNWAY API Gateway** at:

```text
http://localhost:9191
```

The API base URL is currently configured in:

```text
src/app/core/services/api.service.ts
```

```ts
readonly baseUrl = 'http://localhost:9191';
```

The gateway routes requests to the appropriate Spring Boot microservice. Protected requests use JWT authentication, with frontend access restrictions also enforced through Angular route guards.

### Local service addresses

| Component | Local URL |
|---|---|
| Angular frontend | `http://localhost:4200` |
| API Gateway | `http://localhost:9191` |
| Eureka dashboard | `http://localhost:8761` |
| Aggregated Swagger UI | `http://localhost:9191/swagger-ui/index.html` |

## Application Routes

Selected routes defined in `src/app/app.routes.ts`:

| Route | Purpose |
|---|---|
| `/` | Home |
| `/login` | Login |
| `/signup` | Registration |
| `/category/all` | Browse categories/products |
| `/category/:id` | Category-specific products |
| `/product/:id` | Product details |
| `/search` | Product search |
| `/wishlist` | Wishlist |
| `/cart` | Shopping cart |
| `/checkout` | Checkout |
| `/orders` | Order history |
| `/orders/:id` | Order details |
| `/orders/:id/tracking` | Shipment tracking |
| `/account` | Account management |
| `/notifications` | Notifications |
| `/seller-dashboard` | Seller dashboard |
| `/admin-dashboard` | Admin dashboard |
| `/support-dashboard` | Support dashboard |
| `/lookbook` | Lookbook |
| `/our-story` | Brand story |

Some routes require authentication or a particular role. See `app.routes.ts` for the full route definitions.

## Development Commands

Run the development server:

```powershell
npm start
```

Create a production build:

```powershell
npm run build
```

Build artifacts are generated in `dist/`.

Run unit tests:

```powershell
npm test
```

The test command uses Angular's configured test runner (Vitest). The repository does not define a dedicated end-to-end testing script.

## Troubleshooting

**API calls fail:** Verify the backend services and API Gateway are running, then open `http://localhost:8761` to inspect service registrations.

**Login or protected pages fail:** Confirm that the Auth Service is running and that requests are reaching the gateway on port `9191`.

**Products or categories are empty:** Confirm the Catalog Service is running and that its database contains categories and products.

**Product images do not load:** Check the Catalog Service's upload directory and configured image base URL.

**Port 4200 is occupied:** Stop the conflicting process or start Angular on a different port with `npx ng serve --port 4201`.

## Related Documentation

- [RUNWAY project overview](../README.md)
- [RUNWAY backend setup and architecture](../runway_backend/README.md)

## License

No explicit open-source license has been assigned to this repository at this time.
