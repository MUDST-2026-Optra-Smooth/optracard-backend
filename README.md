# OptraCard Backend

OptraCard is an enterprise-grade Trading Card Game (TCG) e-commerce and multi-vendor marketplace platform built with **Spring Boot 3**, **Java 21**, **Spring Security 6**, and **PostgreSQL**.

The backend powers both the **Optracard Official Store** and a **Multi-Store Seller Marketplace**, supporting customer shopping experiences, merchant storefronts, operational administration, and executive super-admin oversight.

---

## Table of Contents
- [Tech Stack](#tech-stack)
- [Architecture & Key Features](#architecture--key-features)
- [Database Architecture](#database-architecture)
- [Security & Authentication](#security--authentication)
  - [Security Configuration & Filter Chain](#security-configuration--filter-chain)
  - [Role-Based Access Control (RBAC)](#role-based-access-control-rbac)
- [API Route Reference](#api-route-reference)
  - [1. Authentication (`/api/auth`)](#1-authentication-apiauth)
  - [2. User Profile (`/api/profile`)](#2-user-profile-apiprofile)
  - [3. Products & Public Catalog (`/api/products`)](#3-products--public-catalog-apiproducts)
  - [4. Marketplace Public Storefront (`/api/marketplace`)](#4-marketplace-public-storefront-apimarketplace)
  - [5. Seller Store Applications (`/api/stores`)](#5-seller-store-applications-apistores)
  - [6. Shopping Cart (`/api/cart`)](#6-shopping-cart-apicart)
  - [7. Customer Orders (`/api/orders`)](#7-customer-orders-apiorders)
  - [8. Seller Portal (`/api/seller`)](#8-seller-portal-apiseller)
  - [9. Admin Operations (`/api/admin`)](#9-admin-operations-apiadmin)
  - [10. Admin Store Reviews (`/api/admin/stores`)](#10-admin-store-reviews-apiadminstores)
  - [11. Admin Marketplace Reviews (`/api/admin/marketplace/requests`)](#11-admin-marketplace-reviews-apiadminmarketplacerequests)
  - [12. Super Admin Executive Suite (`/api/superadmin`)](#12-super-admin-executive-suite-apisuperadmin)
- [Environment Variables & Configuration](#environment-variables--configuration)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Database Setup](#database-setup)
  - [Running the Application](#running-the-application)
  - [Running Tests](#running-tests)
- [Default Demo Accounts](#default-demo-accounts)

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Core Framework** | Spring Boot `3.2.4` |
| **Language** | Java `17` / `21` |
| **Security & Auth** | Spring Security 6, JWT (`jjwt 0.11.5`), BCrypt Password Hashing |
| **Data & Persistence** | Spring Data JPA, Hibernate, PostgreSQL (`postgresql 42.7.2`) |
| **API Documentation** | Springdoc OpenAPI UI (`springdoc-openapi-starter-webmvc-ui 2.3.0`) |
| **Rate Limiting** | Custom in-memory Token Bucket `RateLimitFilter` (configurable per profile) |
| **Testing** | JUnit 5 Jupiter, Mockito, Spring Boot Test / MockMvc |
| **Build Tool** | Apache Maven 3 |

---

## Architecture & Key Features

- **Dual-Model Inventory System**:
  - `OFFICIAL`: Items sold directly by Optracard Official Store.
  - `MARKETPLACE`: Items listed by independent verified merchant stores. Products require administrator approval (`PENDING` -> `APPROVED`) before appearing publicly.
- **Stateless JWT Security**:
  - Every authenticated request is authorized via Bearer JWT token in the `Authorization` header.
  - Granular role boundaries prevent privilege escalation.
- **DDoS & Scraping Protection**:
  - Integrated `RateLimitFilter` guards against burst flooding.
  - Tomcat connection timeouts mitigate slowloris attacks.
  - Maximum upload payload size strictly enforced (5MB).
- **Automated Catalog Seeder (`LocalCatalogSeeder`)**:
  - Under the `local` profile, automatically provisions demo card games (Pokemon, One Piece, Yu-Gi-Oh!, Magic: The Gathering), sample stores, verified sellers, and rich catalog items with high-resolution imagery.

---

## Database Architecture

The PostgreSQL schema is partitioned into core domain tables:

```
  +------------------+         1:N         +----------------------+
  |   Users_Admins   |-------------------->|  Marketplace_Stores  |
  +------------------+                     +----------------------+
       |          |                                   | 1:N
    1:N|       1:1|                                   |
       v          v                                   v
  +--------+   +-------+        1:N               +----------------+
  | Orders |   | Carts |------------------------->|    Products    |
  +--------+   +-------+                          +----------------+
       |          |                                       ^
    1:N|       1:N|                                       | N:1
       v          v                               +----------------+
  +------------+  +-----------+                   |   CardGames    |
  | OrderItems |  | CartItems |                   +----------------+
  +------------+  +-----------+
```

- **`Users_Admins`**: Stores customer, seller, admin, and super-admin accounts with encrypted credentials and roles.
- **`CardGames`**: Supported TCG categories and metadata.
- **`Marketplace_Stores`**: Merchant store profile, owner identity, bank details, and verification state.
- **`Products`**: Inventory items with pricing, cost, stock, type (Single, Booster Pack, Booster Box, Accessories), approval status, and source (`OFFICIAL` vs `MARKETPLACE`).
- **`Carts` & `CartItems`**: Persistent shopping carts per user.
- **`Orders` & `OrderItems`**: Immutable historical transaction records with delivery snapshot details.

---

## Security & Authentication

### Security Configuration & Filter Chain
Configured in `org.example.config.SecurityConfig`:

```
Incoming Request
       │
       ▼
 [CorsFilter]               Configured with dynamic origin patterns:
                            - http://localhost:*
                            - http://127.0.0.1:*
                            Exposes: Authorization, Content-Type, Retry-After
       │
       ▼
[RateLimitFilter]           Enforces request limits per IP (disabled in local dev)
       │
       ▼
[JwtAuthenticationFilter]   Validates Bearer token & sets SecurityContext
       │
       ▼
[AuthorizationFilter]       Evaluates endpoint matchers against user roles
       │
       ▼
[DispatcherServlet]         Routes to target controller
```

### Role-Based Access Control (RBAC)

The platform defines 4 roles:
1. `USER`: Default role upon registration. Can browse catalog, manage personal profile, cart, place orders, and apply for a seller store.
2. `SELLER`: An approved merchant. Can access `/api/seller/**` to list products, view store orders, and update shipment statuses.
3. `ADMIN`: Platform operator. Can manage official store stock, review seller applications, approve/reject marketplace listings, and fulfill platform orders.
4. `SUPER_ADMIN`: Executive administrator. Full platform oversight including GMV analytics, store directory, staff account creation (`ADMIN`), and user account deletion.

---

## API Route Reference

### 1. Authentication (`/api/auth`)
Open to all users without authentication.

| Method | Endpoint | Description | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | Register a new customer account | `RegisterRequest` (`username`, `email`, `password`, `phone`, `address`) | `200 OK` (Success message) |
| `POST` | `/api/auth/login` | Authenticate with email and password | `AuthRequest` (`email`, `password`) | `200 OK` (`AuthResponse` with JWT token, user info, role) |

---

### 2. User Profile (`/api/profile`)
Requires authenticated user (`USER`, `SELLER`, `ADMIN`, `SUPER_ADMIN`).

| Method | Endpoint | Description | Request Body | Response |
|---|---|---|---|---|
| `GET` | `/api/profile` | Retrieve profile and card collection of authenticated user | None | `200 OK` (`ProfileResponse`) |
| `PUT` | `/api/profile` | Update profile information (username, email, phone, address) | `ProfileUpdateRequest` | `200 OK` (`ProfileUpdateResponse` with refreshed token) |

---

### 3. Products & Public Catalog (`/api/products`)
Public catalog browsing is open; product creation requires `ADMIN` or `SUPER_ADMIN`.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/products` or `/api/products/home` | Public | Returns curated catalog products for the storefront home page |
| `GET` | `/api/products/{id}` | Public | Returns detailed product information by ID |
| `GET` | `/api/products/{id}/offers` | Public | Returns alternative merchant marketplace offers for the same card |
| `GET` | `/api/products/search?q={keyword}` | Public | Performs case-insensitive search across product name, game, and type |
| `POST` | `/api/products` | `ADMIN`, `SUPER_ADMIN` | Creates a new official store product listing |

---

### 4. Marketplace Public Storefront (`/api/marketplace`)
Publicly view verified seller storefronts.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/marketplace/stores/{storeId}` | Public | Returns public store profile, owner details, and approved product inventory |

---

### 5. Seller Store Applications (`/api/stores`)
Requires authenticated user (`USER`, `SELLER`, etc.).

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| `GET` | `/api/stores/my` | Retrieve the authenticated user's merchant store application | None |
| `POST` | `/api/stores/my` | Submit a new seller store application | `StoreApplicationRequest` (store name, slug, address, bank info, terms) |
| `PUT` | `/api/stores/my` | Update an existing seller store application | `StoreApplicationRequest` |

---

### 6. Shopping Cart (`/api/cart`)
Requires authenticated user.

| Method | Endpoint | Query / Path Parameters | Description |
|---|---|---|---|
| `GET` | `/api/cart/{userId}` | `userId` (in path) | Returns cart contents, line items, and total count |
| `POST` | `/api/cart/add` | `?userId={id}&productId={id}&quantity={n}` | Adds a product to user's cart (validates inventory stock) |
| `PUT` | `/api/cart/update` | `?userId={id}&cartItemId={id}&quantity={n}` | Updates item quantity in cart |
| `DELETE` | `/api/cart/remove` | `?cartItemId={id}` | Removes a specific item from cart |
| `DELETE` | `/api/cart/clear/{userId}` | `userId` (in path) | Clears all items in the user's cart |

---

### 7. Customer Orders (`/api/orders`)
Requires authenticated user.

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| `GET` | `/api/orders` | Retrieves all past orders placed by the authenticated customer | None |
| `GET` | `/api/orders/{orderNumber}` | Retrieves full order details by order number | None |
| `POST` | `/api/orders` | Checkout and place an order (creates order items, clears cart items) | `CreateOrderRequest` (recipient, shipping address, payment method) |

---

### 8. Seller Portal (`/api/seller`)
Restricted to `SELLER`, `ADMIN`, `SUPER_ADMIN`.

| Method | Endpoint | Description | Request / Body |
|---|---|---|---|
| `GET` | `/api/seller/games` | Retrieve available card games for listing creation | None |
| `GET` | `/api/seller/products` | Retrieve all marketplace products listed by this seller | None |
| `GET` | `/api/seller/products/{id}` | Retrieve specific product listed by this seller | None |
| `POST` | `/api/seller/products` | Create a new marketplace product (sets status to `PENDING` review) | `SellerProductRequest` |
| `PUT` | `/api/seller/products/{id}` | Update product details (resets status to `PENDING` review) | `SellerProductRequest` |
| `DELETE` | `/api/seller/products/{id}` | Soft-deactivate a marketplace listing | None |
| `GET` | `/api/seller/orders` | Retrieve customer orders assigned to this seller's store | None |
| `PUT` | `/api/seller/orders/{id}/status` | Update fulfillment status (`SHIPPED`, `DELIVERED`, `CANCELLED`) | Body: `{ "status": "..." }` |

---

### 9. Admin Operations (`/api/admin`)
Restricted to `ADMIN` and `SUPER_ADMIN`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/dashboard` | Dashboard metrics: total revenue, expenses, net profit, orders, and 12-month metrics |
| `GET` | `/api/admin/card-games` | List all supported card games |
| `GET` | `/api/admin/products` | List all official store products |
| `GET` | `/api/admin/products/{id}` | Get specific official product details |
| `POST` | `/api/admin/products` | Add new official product |
| `PUT` | `/api/admin/products/{id}` | Update official product details |
| `DELETE` | `/api/admin/products/{id}` | Deactivate official product |
| `GET` | `/api/admin/orders` | List all platform orders across official and marketplace stores |
| `GET` | `/api/admin/orders/{id}` | View detailed order breakdown |
| `PUT` | `/api/admin/orders/{id}/status` | Update order status |
| `GET` | `/api/admin/stores?status={ALL\|PENDING\|APPROVED\|REJECTED}` | List merchant store applications with status filter |
| `GET` | `/api/admin/stores/{id}` | View store application details |
| `GET` | `/api/admin/marketplace/products` | List all marketplace products across all stores |
| `GET` | `/api/admin/marketplace/products/{id}` | View individual marketplace product |

---

### 10. Admin Store Reviews (`/api/admin/stores`)
Restricted to `ADMIN` and `SUPER_ADMIN`.

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| `PUT` | `/api/admin/stores/{id}/review` | Approve or reject a seller store application | `{ "status": "APPROVED" \| "REJECTED", "note": "..." }` |

---

### 11. Admin Marketplace Reviews (`/api/admin/marketplace/requests`)
Restricted to `ADMIN` and `SUPER_ADMIN`.

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| `GET` | `/api/admin/marketplace/requests?status={ALL\|PENDING\|APPROVED\|REJECTED}` | List seller listing review requests | None |
| `PUT` | `/api/admin/marketplace/requests/{id}/approve` | Approve a marketplace product listing | None |
| `PUT` | `/api/admin/marketplace/requests/{id}/reject` | Reject a marketplace product listing | `{ "note": "..." }` (Optional) |

---

### 12. Super Admin Executive Suite (`/api/superadmin`)
Restricted exclusively to `SUPER_ADMIN`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/superadmin/overview` | Platform-wide KPIs: 30-day GMV, total stores, active listings, inventory distribution, monthly revenue graph, top performing stores |
| `GET` | `/api/superadmin/stores` | Comprehensive list of all stores and their metrics |
| `GET` | `/api/superadmin/stores/{storeId}` | Deep-dive store performance report and listing breakdown |
| `GET` | `/api/superadmin/catalog` | Full platform catalog inspection (both official and marketplace) |
| `GET` | `/api/superadmin/transactions` | Full audit log of all financial orders and customer transactions |
| `GET` | `/api/superadmin/users` | List all customer and merchant user accounts |
| `DELETE` | `/api/superadmin/users/{userId}` | Delete user account (requires confirmation body `{"confirmation": "DELETE"}`) |
| `GET` | `/api/superadmin/staff` | List all administrator accounts (`ADMIN` and `SUPER_ADMIN`) |
| `POST` | `/api/superadmin/staff` | Create a new `ADMIN` staff account |
| `DELETE` | `/api/superadmin/staff/{userId}` | Delete an administrator account (protects last `SUPER_ADMIN`) |

---

## Environment Variables & Configuration

Configuration files are located in `src/main/resources/`:
- `application.properties`: Shared configuration across all environments.
- `application-local.properties`: Default settings for local development.

### Common Configuration Keys

| Key | Description | Default (Local) |
|---|---|---|
| `spring.profiles.active` | Active Spring profile | `local` |
| `spring.datasource.url` | JDBC connection string | `jdbc:postgresql://localhost:5432/optracard` |
| `spring.datasource.username` | Database username | `admin` |
| `spring.datasource.password` | Database password | `password` |
| `spring.jpa.hibernate.ddl-auto` | Hibernate DDL generation mode | `update` |
| `jwt.secret` | Secret key for JWT signing / HMAC validation | `optracard-local-jwt-secret-change-this-value` |
| `app.rate-limit.enabled` | Enable/disable RateLimitFilter | `false` (in local) |
| `springdoc.swagger-ui.path` | Swagger UI web path | `/swagger-ui.html` |
| `springdoc.api-docs.path` | OpenAPI JSON schema path | `/v3/api-docs` |

---

## Getting Started

### Prerequisites
- **Java 17** or **Java 21**
- **Maven 3.8+**
- **PostgreSQL 14+**

### Database Setup
1. Create a PostgreSQL database named `optracard`:
   ```sql
   CREATE DATABASE optracard;
   ```
2. Run `database/init.sql` to initialize tables, constraints, and relational schemas.
3. (Optional) Run `database/seed-seller-account.sql` if you want predefined test accounts.

### Running the Application

Using Maven:
```bash
mvn clean spring-boot:run
```

The application will start on port `8080`.
- Swagger UI Documentation: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON Specification: `http://localhost:8080/v3/api-docs`

### Running Tests

Execute the complete test suite (Unit tests, Controller WebMvc tests, Security configuration tests, Service tests):
```bash
mvn test
```

---

## Default Demo Accounts

When running in `local` profile, the following demo accounts are available:

| Email | Password | Role | Description |
|---|---|---|---|
| `superadmin@optracard.com` | `password123` | `SUPER_ADMIN` | Platform Super Administrator |
| `ops@optracard.com` | `password123` | `ADMIN` | Operations Administrator |
| `seller.demo@optracard.local` | `SellerDemo!2026` | `SELLER` | Approved Merchant (`AAA-Trading`) |
| `somchai@optracard.com` | `password123` | `USER` | Regular Customer Account |
