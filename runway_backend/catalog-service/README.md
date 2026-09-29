# INTISG26JFAGN001_Team2_OnlineShoppingApp_CatalogService

# Catalog Service

Catalog Service is the product catalog microservice for the Online Shopping microservices platform.  
It manages product categories, products, product media, and inventory.

## Service Snapshot

- **Module:** `catalog-service`
- **Application name:** `CATALOG-SERVICE`
- **Default port:** `8765`
- **Tech stack:** Spring Boot, Spring Web, Spring Security, Spring Data JPA, Flyway, MySQL, Eureka Client, OpenAPI/Swagger
- **Java version:** 21

## Role in the Overall Platform

This service participates in a Eureka-registered microservices architecture alongside:
- `service-registry` (Eureka server)
- `api-gateway` (routing + auth header injection)
- `auth-service` (token validation)
- `order-service`, `payment-service`, `customer-engagement-service`

Gateway routes `/catalog/**` to this service and injects identity headers after token validation.

## Core Capabilities

- **Category Management**
    - Create category (supports parent-child hierarchy)
    - List root categories
- **Product Management**
    - Create product (multipart request with optional images)
    - Retrieve by id / list all / list by category / list by seller
    - Update and delete with ownership checks
- **Inventory Management**
    - Get product stock
    - Adjust stock with role + ownership rules
- **Media Handling**
    - Store uploaded product images on filesystem
    - Expose images via static resource mapping

## API Base Paths

- `/catalog/categories`
- `/catalog/products`
- `/catalog/inventory`
- Static images: `/api/v1/catalog/images/**`

## Security & Authorization Model

- Spring Security is enabled with method-level security.
- A custom filter reads gateway-provided headers (not direct JWT parsing in this service):
    - `X-User-Id`
    - `X-User-Role`
- Access patterns:
    - Category creation: `ADMIN`
    - Product create/update: `SELLER` or `ADMIN`
    - Product/inventory updates also enforce seller ownership unless role is `ADMIN`

## Persistence & Schema

Flyway migration: `src/main/resources/db/migration/V1__Initial_Catalog_Schema.sql`

Tables:
- `categories` (self-referencing hierarchy)
- `products` (includes JSON `attributes`)
- `inventory` (one-to-one with product)
- `media` (one-to-many with product)

## Configuration Highlights

From `src/main/resources/application.properties` and `application-dev.properties`:

- `server.port=8765`
- `spring.profiles.active=dev`
- Eureka registration enabled (`http://localhost:8761/eureka`)
- MySQL dev DB: `catalog_db`
- Flyway enabled
- Upload directory: `app.upload.dir`
- Image URL base: `app.image.base-url`
- Swagger:
    - `/swagger-ui.html`
    - `/v3/api-docs`

## Build, Run, Test

> These are standard project commands; adapt paths/credentials to your environment.

```powershell
cd "D:\runway\runway_backend\catalog-service"
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
