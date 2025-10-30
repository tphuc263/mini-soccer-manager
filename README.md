# Mini Soccer Management — Backend

## Overview
- RESTful API powering field bookings, payments, and admin workflows for the Mini Soccer Management platform.
- Built with Spring Boot 3.5 (Java 21) and secured by JWT-based authentication with role-aware authorization.
- Integrates with VNPay for online payments while supporting COD/manual settlement paths.

## Tech Stack
- Spring Boot 3.5 (Web, Data JPA, Validation, Security)
- Java 21
- Maven Wrapper (`./mvnw`)
- MySQL 8.x
- JSON Web Tokens (JJWT 0.12.x)

## Key Features
- **Authentication & Authorization** — Phone-number login, JWT issuance, and `ADMIN` / `USER` role separation.
- **Field Management** — CRUD endpoints for field inventory and pricing (admin scoped).
- **Booking Lifecycle** — Create, list, and cancel bookings with overlap prevention and ownership checks.
- **Payments** — VNPay payment initiation and callback handling plus COD/manual payment updates.
- **Admin Dashboards** — Aggregated booking and cancellation views for back-office teams.
- **Global Error Handling** — Consistent API responses via centralized exception management.

## Project Structure
- `src/main/java/com/mini/soccer/controller` — REST controllers (`Auth`, `Field`, `Booking`, `Admin`, `VNPay` callback).
- `src/main/java/com/mini/soccer/service` — Domain services for booking, field, and payment logic.
- `src/main/java/com/mini/soccer/security` — JWT utilities, filters, and Spring Security configuration.
- `src/main/java/com/mini/soccer/model` — JPA entities (`User`, `Field`, `Booking`, `Payment`).
- `src/main/java/com/mini/soccer/dto` — DTOs for request/response payloads.
- `src/main/resources/application.yml` — Default configuration (override with env vars in deployment).

## Prerequisites
1. Java 21 (`java -version`)
2. Maven 3.9+ (or rely on the provided Maven Wrapper)
3. MySQL 8.x server

## Configuration
Update `src/main/resources/application.yml` or provide environment variables when starting the app. Core properties:

| Property / Env Var | Purpose | Default |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:mysql://localhost:3306/soccer_management_db` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `socceruser` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `soccerpass` |
| `AUTH_JWT_SECRET` | Base64-encoded JWT signing secret | `YXNzZWN1cmUtZGVmYXVsdC1qd3Qtc2VjcmV0LXN0cmluZw==` |
| `AUTH_ACCESS_EXPIRATION` | Access token lifetime (ms) | `86400000` (24 hours) |
| `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` / `VNPAY_PAY_URL` / ... | VNPay credentials | Sandbox defaults |
| `FRONTEND_VNPAY_CALLBACK_URL` | FE URL for VNPay redirects | `http://localhost:3000/payment/vnpay/callback` |

> `spring.jpa.hibernate.ddl-auto` is set to `none`. Provision the schema manually (via migrations or SQL scripts) before running the service. Switch to `update` only for local experimentation.

## Database Setup (Local)
```sql
CREATE DATABASE soccer_management_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'socceruser'@'%' IDENTIFIED BY 'soccerpass';
GRANT ALL PRIVILEGES ON soccer_management_db.* TO 'socceruser'@'%';
FLUSH PRIVILEGES;
```
Populate tables using your preferred migration/seeding approach.

## Getting Started
1. Install dependencies and configure the database.
2. Run the service:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Access the API at `http://localhost:8080/api/v1`.
4. Execute tests:
   ```bash
   ./mvnw test
   ```

## API Highlights
- `POST /api/v1/auth/login` — Authenticate and receive JWT.
- `POST /api/v1/auth/register` — Register new users.
- `GET /api/v1/fields` — Public field catalogue.
- `POST /api/v1/fields` — Create field (admin only).
- `POST /api/v1/bookings` — Reserve a field (authenticated user).
- `POST /api/v1/bookings/{id}/cancel` — Cancel a booking (owner or admin).
- `POST /api/v1/bookings/{id}/pay` — Initiate payment (VNPay or manual).
- `POST /api/v1/payments/vnpay/callback` — VNPay callback endpoint (public).
- `GET /api/v1/admin/bookings` — Paginated overview for admins.

Refer to controller classes under `src/main/java/com/mini/soccer/controller` for the full contract.

## Development Notes
- Adjust the CORS configuration in `SecurityConfig` before deploying to locked-down environments.
- Keep secrets out of source control—prefer environment variables or an external config store.
- Re-run `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` (or similar) if you add profile-specific configs.

