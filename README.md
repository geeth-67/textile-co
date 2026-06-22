# TextileCo Microservices

A production-style microservices project for a textile company management system. The architecture demonstrates **API Gateway routing**, **Keycloak OAuth2/JWT authentication**, **PostgreSQL per-service databases**, and **Docker containerization**.

## Architecture

```
                    ┌─────────────────┐
                    │     Client      │
                    │  (curl/Postman) │
                    └────────┬────────┘
                             │ Bearer JWT
                             ▼
                    ┌─────────────────┐
                    │   API Gateway   │  :9000
                    │ Spring Cloud GW │
                    │  + OAuth2 JWT   │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Product Service │ │  Order Service  │ │Customer Service │
│     :8081       │ │     :8082       │ │     :8083       │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  products-db    │ │   orders-db     │ │  customers-db   │
│  PostgreSQL     │ │  PostgreSQL     │ │  PostgreSQL     │
└─────────────────┘ └─────────────────┘ └─────────────────┘

         ┌─────────────────┐
         │    Keycloak     │  :9001
         │ realm: textileco│
         └─────────────────┘
```

| Component | Port | Description |
|-----------|------|-------------|
| API Gateway | 9000 | Single entry point, JWT validation |
| Product Service | 8081 | Fabric product catalog |
| Order Service | 8082 | Customer orders |
| Customer Service | 8083 | Customer records |
| Keycloak | 9001 | OAuth2 / OpenID Connect |
| products-db | 5432 | Product database |
| orders-db | 5433 | Order database |
| customers-db | 5434 | Customer database |

## Prerequisites

- **Java 21** (JDK)
- **Maven 3.9+**
- **Docker** and **Docker Compose**
- **curl** or **Postman** for API testing

## Project Structure

```
textileco/
├── docker-compose.yml
├── keycloak/
│   └── realm-export.json
├── api-gateway/
├── product-service/
├── order-service/
└── customer-service/
```

Each service is a standalone Maven module with:
- `pom.xml`, `Dockerfile`
- `entity/`, `repository/`, `service/`, `controller/`
- `application.yaml`

## How to Run PostgreSQL

PostgreSQL runs automatically via Docker Compose. Three separate databases are created:

| Database | Container | Host Port |
|----------|-----------|-----------|
| products | products-db | 5432 |
| orders | orders-db | 5433 |
| customers | customers-db | 5434 |

Credentials: `textileco` / `textileco123`

```bash
# Start only databases
docker compose up -d products-db orders-db customers-db
```

## How to Run Keycloak

Keycloak starts with the `textileco` realm pre-imported from `keycloak/realm-export.json`.

| Setting | Value |
|---------|-------|
| Admin Console | http://localhost:9001 |
| Master Admin | admin / admin |
| Realm | textileco |
| Test User | admin / admin123 |
| Client | gateway-client |
| Client Secret | `aooNNUcCJGT8iKDaNftVhqHhHclivLi0` |

```bash
docker compose up -d keycloak keycloak-init
```

The `keycloak-init` container sets the `admin` user password to `admin123` after Keycloak is healthy.

## How to Run All Services

### Option 1: Docker Compose (recommended)

```bash
cd textileco
docker compose up -d
```

Wait ~60 seconds for all services to start, then verify:

```bash
docker compose ps
curl http://localhost:9000/actuator/health
```

### Option 2: Local development

1. Start infrastructure:
   ```bash
   docker compose up -d products-db orders-db customers-db keycloak keycloak-init
   ```

2. Run each service in a separate terminal:
   ```bash
   cd product-service && ./mvnw spring-boot:run
   cd order-service && ./mvnw spring-boot:run
   cd customer-service && ./mvnw spring-boot:run
   cd api-gateway && ./mvnw spring-boot:run
   ```

## Docker Commands

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# View logs for a specific service
docker compose logs -f api-gateway

# Stop all services
docker compose down

# Stop and remove volumes (reset databases)
docker compose down -v

# Rebuild images after code changes
docker compose up -d --build

# Check service status
docker compose ps
```

## Obtain JWT Token

All API requests must include a valid JWT from Keycloak.

```bash
curl -s -X POST "http://localhost:9001/realms/textileco/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=gateway-client" \
  -d "client_secret=aooNNUcCJGT8iKDaNftVhqHhHclivLi0" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "scope=products.read products.write orders.read orders.write customers.read customers.write" \
  | jq -r '.access_token'
```

Save the token:

```bash
export TOKEN=$(curl -s -X POST "http://localhost:9001/realms/textileco/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=gateway-client" \
  -d "client_secret=aooNNUcCJGT8iKDaNftVhqHhHclivLi0" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "scope=products.read products.write orders.read orders.write customers.read customers.write" \
  | jq -r '.access_token')
```

## Test Product API

```bash
# Create a product
curl -X POST http://localhost:9000/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Cotton Shirt",
    "fabricType": "Cotton",
    "color": "Blue",
    "price": 29.99,
    "stock": 100
  }'

# Get all products
curl http://localhost:9000/products \
  -H "Authorization: Bearer $TOKEN"

# Get product by ID
curl http://localhost:9000/products/1 \
  -H "Authorization: Bearer $TOKEN"

# Update product
curl -X PUT http://localhost:9000/products/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Cotton Shirt Premium",
    "fabricType": "Cotton",
    "color": "Navy",
    "price": 34.99,
    "stock": 80
  }'

# Delete product
curl -X DELETE http://localhost:9000/products/1 \
  -H "Authorization: Bearer $TOKEN"
```

## Test Order API

```bash
# Create an order
curl -X POST http://localhost:9000/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "customerName": "John Doe",
    "quantity": 2,
    "orderDate": "2026-06-22"
  }'

# Get all orders
curl http://localhost:9000/orders \
  -H "Authorization: Bearer $TOKEN"

# Get order by ID
curl http://localhost:9000/orders/1 \
  -H "Authorization: Bearer $TOKEN"
```

## Test Customer API

```bash
# Create a customer
curl -X POST http://localhost:9000/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Smith",
    "email": "jane@textileco.com",
    "address": "123 Fabric Street, Colombo"
  }'

# Get all customers
curl http://localhost:9000/customers \
  -H "Authorization: Bearer $TOKEN"

# Get customer by ID
curl http://localhost:9000/customers/1 \
  -H "Authorization: Bearer $TOKEN"
```

## Troubleshooting Guide

### 401 Unauthorized from Gateway

- Ensure you include `Authorization: Bearer <token>` header
- Token may have expired (default lifespan: 5 minutes) — obtain a new token
- Verify Keycloak is running: `curl http://localhost:9001/realms/textileco/.well-known/openid-configuration`

### 403 Forbidden from Gateway

- The JWT lacks required scopes or roles
- Use the `admin` user (has `ROLE_admin`) or request appropriate scopes when obtaining the token

### Services fail to start / database connection errors

- Wait for PostgreSQL health checks to pass before services start
- Check logs: `docker compose logs product-service`
- Verify database is healthy: `docker compose ps`

### Keycloak realm not imported

- Check Keycloak logs: `docker compose logs keycloak`
- Ensure `keycloak/realm-export.json` is mounted correctly
- Re-import: `docker compose down -v && docker compose up -d`

### admin/admin123 login fails

- Run the init container: `docker compose up keycloak-init`
- Or set password manually in Keycloak Admin Console → Users → admin → Credentials

### Gateway cannot validate JWT in Docker

- Inside Docker, the gateway uses `http://keycloak:8080/realms/textileco` (set via environment variables in `docker-compose.yml`)
- For local development, `application.yaml` uses `http://localhost:9001/realms/textileco`

### Port already in use

```bash
# Find process using port 9000
lsof -i :9000
# Stop conflicting containers
docker compose down
```

### Rebuild after code changes

```bash
docker compose up -d --build api-gateway product-service order-service customer-service
```

## Security Notes

- JWT validation is enforced **only at the API Gateway** (same pattern as the reference architecture)
- Microservices do not validate tokens directly — they trust internal Docker network traffic
- Never expose microservice ports publicly in production; route all traffic through the gateway

## Tech Stack

- Java 21
- Spring Boot 3.5.14
- Spring Cloud Gateway 2025.0.2
- Spring Security OAuth2 Resource Server
- Keycloak 25.0.0
- Spring Data JPA
- PostgreSQL 17
- Docker & Docker Compose
- Lombok
- Maven
