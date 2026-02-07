# Tax Backend - Complete Architecture Analysis

## Executive Summary

**Taxool Backend** is a Domain-Driven Design (DDD) application for automating Polish cryptocurrency capital gains tax calculations (PIT). The system integrates with brokers (currently Coinbase), normalizes orders into a broker-agnostic format, and generates tax reports through an event-driven flow.

**Tech Stack:**
- Java 21
- Spring Boot 3.3.1
- PostgreSQL (with Flyway migrations)
- AWS Cognito (OAuth2/JWT authentication)
- Gradle build system
- Docker deployment to AWS EC2

---

## Core Business Flow

```
┌────────────┐
│  1. User   │  Integrates broker via OAuth
│   OAuth    │  → Tokens stored in DB
└─────┬──────┘
      │
      ▼
┌────────────┐
│ 2. Sync    │  POST /api/broker/coinbase/orders/sync
│   Orders   │  → Fetches transactions from Coinbase API
└─────┬──────┘
      │
      ▼
┌────────────────────────────────────────────────────────┐
│                  Event-Driven Pipeline                  │
│                                                          │
│  NewOrdersEvent → UserOrdersChangedEvent →              │
│  → CapitalGainsReportUpdatedEvent                       │
└─────┬────────────────────────────────────────────────┬──┘
      │                                                  │
      ▼                                                  ▼
┌────────────┐                                    ┌────────────┐
│ 3. Orders  │  Broker-agnostic normalized       │ 4. Capital │
│   Domain   │  orders saved to DB                │   Gains    │
│            │  (idempotent by externalId)        │            │
└────────────┘                                    └─────┬──────┘
                                                        │
                                                        ▼
                                              ┌─────────────────┐
                                              │ 5. PIT Report   │
                                              │  (Polish Tax)   │
                                              │  - Converts PLN │
                                              │  - Uses NBP API │
                                              └─────────────────┘
```

---

## Domain Architecture (DDD)

### Layering Pattern (Applied Consistently Across All Domains)

```
┌─────────────────────────────────────────────────────────┐
│              Application Layer                          │
│  - Controllers (REST API)                               │
│  - Event Consumers (@EventListener)                     │
│  - Wiring Configuration (@Configuration)                │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│              Domain Layer                                │
│  - Facade (Action/Query Router)                         │
│  - Use Cases (Single Responsibility)                    │
│  - Domain Models (Aggregates/Entities)                  │
│  - Ports (Interfaces for infrastructure)                │
│  - Actions/Queries (Input DTOs)                         │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│           Infrastructure Layer                           │
│  - Adapters (Implement Ports)                           │
│  - JPA Repositories                                      │
│  - HTTP Clients (RestClient)                            │
│  - External API Integrations                            │
└──────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.mateuszcer.taxbackend/
├── brokers/                 # Broker integration domain
│   ├── application/         # BrokerController, consumers
│   ├── domain/              # BrokerFacade, use cases, ports
│   ├── coinbase/            # Coinbase adapter (infrastructure)
│   │   ├── adapter/
│   │   ├── infrastructure/  # CoinbaseClient (HTTP)
│   │   └── model/
│   └── infrastructure/      # Broker connection JPA
│
├── orders/                  # Normalized orders domain
│   ├── application/         # OrderController, NewOrdersEventConsumer
│   ├── domain/              # OrderFacade, Order entity, use cases
│   └── infrastructure/      # OrderRepository (JPA)
│
├── capitalgains/            # Tax calculations (country-agnostic)
│   ├── application/         # UserOrdersChangedEventConsumer
│   ├── domain/              # CapitalGainsFacade, FIFO logic
│   └── infrastructure/      # OrdersJpaProvider, report repository
│
├── pit/                     # Polish tax reports (PL-specific)
│   ├── application/         # PitController, CapitalGainsReportUpdatedEventConsumer
│   ├── domain/              # PitFacade, use cases, currency conversion
│   └── infrastructure/      # PitReportRepository, NBP API client
│
├── security/                # Authentication domain
│   ├── application/         # AuthController
│   ├── domain/              # AuthService interface, AuthenticationException
│   └── infrastructure/      # DevAuthService, CognitoAuthService, TestAuthService
│
└── shared/                  # Cross-cutting concerns
    ├── authuserid/          # @AuthUserId annotation & resolver
    ├── config/              # CORS, OpenAPI, Frontend config
    ├── events/              # Domain events (contracts)
    ├── exception/           # GlobalExceptionHandler, custom exceptions
    ├── interceptors/        # Logging, request filters
    └── response/            # ApiResponse<T> envelope
```

---

## Domain Deep-Dive

### 1. **Brokers Domain**

**Responsibility:** Integrate with external brokers (currently Coinbase), handle OAuth, fetch transactions.

**Key Components:**
- `BrokerFacade` - Routes actions/queries to use cases
- `Broker` enum - Supported brokers (COINBASE)
- `BrokerAdapter` port - Contract for broker integrations
- `SyncBrokerOrders` use case - Fetches orders and publishes `NewOrdersEvent`

**Coinbase Adapter (Infrastructure):**
```
CoinbaseAdapter (BrokerAdapter implementation)
    ├── CoinbaseAuthenticationService (BrokerClient impl)
    ├── CoinbaseTransactionService (TransactionMapper impl)
    └── CoinbaseClient (Low-level HTTP client)
         ├── OAuth endpoints
         ├── Token refresh
         └── Orders endpoint
```

**API Endpoints:**
- `GET /api/broker/{brokerId}/auth/url` - Get OAuth URL
- `GET /api/broker/{brokerId}/auth/callback` - OAuth callback (GET for frontend compatibility)
- `POST /api/broker/{brokerId}/auth/exchange` - Exchange OAuth code (POST recommended)
- `POST /api/broker/{brokerId}/orders/sync` - Sync orders from broker
- `GET /api/broker/{brokerId}/orders` - Get user's orders from broker

**Event Published:** `NewOrdersEvent`

---

### 2. **Orders Domain**

**Responsibility:** Centralized, broker-agnostic storage of user orders.

**Key Components:**
- `Order` entity (JPA) - Normalized order model
- `OrderFacade` - Routes actions/queries
- `SaveNewOrders` use case - Idempotent order persistence (by `externalId` + `userId`)
- `GetUserOrders` use case - Retrieve user orders

**Unique Constraint:**
```sql
CONSTRAINT uq_orders_user_external UNIQUE (user_id, external_id)
```

**Indexes:**
- `idx_orders_user_id` - Fast user lookups
- `idx_orders_user_id_occurred_at` - Efficient time-range queries

**API Endpoints:**
- `GET /api/orders` - Get all orders for authenticated user

**Event Flow:**
- Consumes: `NewOrdersEvent` (from brokers)
- Publishes: `UserOrdersChangedEvent` (when orders change)

**Event Consumer:**
```java
@EventListener
public void on(NewOrdersEvent event) {
    // Save orders to DB
    orderFacade.handle(new SaveNewOrdersAction(...));
    
    // Extract affected tax years
    List<Integer> years = extractYears(event.orders());
    
    // Publish downstream event
    publisher.publishEvent(new UserOrdersChangedEvent(userId, years));
}
```

---

### 3. **Capital Gains Domain**

**Responsibility:** Calculate cost basis, proceeds, and gains using FIFO (First-In-First-Out) method.

**Key Components:**
- `CapitalGainsFacade` - Routes actions/queries
- `CalculateCapitalGainsPreview` - Real-time preview (no persistence)
- `GenerateCapitalGainsReport` - Save report to DB
- `UserOrdersProvider` port - Source of orders (implemented by `OrdersJpaProvider`)

**FIFO Algorithm (in `CalculateCapitalGainsPreview`):**
```java
// Maintain lot queue per product
Map<String, Deque<Lot>> lotsByProduct = new HashMap<>();

for (Order order : orders) {
    if (order.side == BUY) {
        // Add lot to queue
        lots.addLast(new Lot(quantity, unitCost));
    } else if (order.side == SELL) {
        // Match against oldest lots (FIFO)
        while (remaining > 0 && !lots.isEmpty()) {
            Lot lot = lots.peekFirst();
            BigDecimal take = remaining.min(lot.qtyRemaining);
            cost += take * lot.unitCost;
            lot.qtyRemaining -= take;
            remaining -= take;
            if (lot.qtyRemaining == 0) lots.removeFirst();
        }
        proceeds += sellProceeds;
    }
}

gain = proceeds - cost;
```

**Report Model:**
```java
record CapitalGainsPreview(
    int taxYear,
    BigDecimal cost,      // Total purchase cost
    BigDecimal proceeds,  // Total sales revenue
    BigDecimal gain,      // proceeds - cost
    List<String> warnings // e.g., "Missing buy lots"
)
```

**Event Flow:**
- Consumes: `UserOrdersChangedEvent` (from orders)
- Publishes: `CapitalGainsReportUpdatedEvent` (when report generated)

**No API Endpoints** - Calculations happen automatically via events

---

### 4. **PIT Domain**

**Responsibility:** Polish tax-specific logic - converts USD to PLN using official NBP rates.

**Key Components:**
- `PitFacade` - Routes actions/queries
- `CalculatePitPreview` - Calculate PIT preview **in PLN**
- `GeneratePitReport` - Save PIT report to DB
- `CurrencyConversionService` - Domain service for USD→PLN conversion
- `ExchangeRateProvider` port - Interface for exchange rates
- `NbpExchangeRateClient` - NBP (Polish National Bank) API adapter

**Currency Conversion Flow:**
```java
// For each transaction:
1. Extract currency from productId ("BTC-USD" → "USD")
2. Get transaction date
3. Fetch NBP rate for that date
4. Convert: amount_usd * nbp_rate = amount_pln
5. Use PLN amounts in FIFO calculations
```

**NBP API Integration:**
```
GET https://api.nbp.pl/api/exchangerates/rates/a/usd/2025-01-15/?format=json

Response:
{
  "rates": [{
    "mid": 4.0524,  // 1 USD = 4.0524 PLN
    "effectiveDate": "2025-01-15"
  }]
}
```

**Caching:**
- Cache: Caffeine (in-memory)
- TTL: 1 hour
- Key: `{currency}_{date}` (e.g., "USD_2025-01-15")
- Max size: 1000 entries

**API Endpoints:**
- `GET /api/pit/{taxYear}/preview` - Preview PIT calculation **in PLN**
- `POST /api/pit/{taxYear}/generate` - Generate and save PIT report

**Event Flow:**
- Consumes: `CapitalGainsReportUpdatedEvent` (from capitalgains)
- No events published (end of chain)

**Key Difference from Capital Gains:**
- Capital Gains: Country-agnostic, works in USD
- PIT: Poland-specific, converts to PLN using NBP rates

---

### 5. **Security Domain**

**Responsibility:** User authentication (sign up, sign in, confirmation).

**Architecture:**
```
AuthController
     ↓ uses
AuthService (interface)
     ↓ implementations
     ├── DevAuthService (@Profile "dev")
     │    └── Accepts ANY password, returns fake tokens
     ├── TestAuthService (@Profile "test")
     │    └── Similar to dev, for unit tests
     └── CognitoAuthService (@Profile "!dev & !test")
          └── Real AWS Cognito integration
```

**API Endpoints:**
- `POST /auth/signUp` - Register new user
- `POST /auth/confirm` - Confirm email with code
- `POST /auth/signIn` - Sign in and get JWT tokens

**Authentication Models:**
- **Dev Mode:** Any email/password → fake JWT (format: `dev_{uuid}`)
- **Prod Mode:** AWS Cognito → real JWT with `sub` claim

**JWT Structure (Dev):**
```json
{
  "header": {"alg": "HS256", "typ": "JWT"},
  "payload": {
    "sub": "dev_abc123456789",
    "email": "user@example.com",
    "iat": 1706640000,
    "exp": 1706726400
  },
  "signature": "fake-signature"
}
```

**User ID Resolution:**
```java
@AuthUserId String userId
```
- Prod: Extracts `sub` claim from JWT
- Dev/Test: Falls back to `X-User-Id` header

**Security Configurations:**
- **DevSecurityConfiguration** (@Profile "dev")
  - All endpoints `permitAll()`, no JWT validation
- **SecurityConfiguration** (@Profile "!dev")
  - OAuth2 resource server, JWT validation via Cognito
  - Public endpoints: `/auth/**`, `/actuator/health`, Swagger

---

## Shared Components

### 1. **ApiResponse Envelope**

Standardized response format for all endpoints:

```java
@Schema(name = "ApiResponse")
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;              // Present on success
    private final String errorCode;    // Present on error
    private final LocalDateTime timestamp;
    
    // Factory methods
    static ApiResponse success(T data);
    static ApiResponse success(T data, String message);
    static ApiResponse error(String message, String errorCode);
}
```

**Success Example:**
```json
{
  "success": true,
  "message": "Orders retrieved successfully",
  "data": [/* orders */],
  "timestamp": "2025-01-30T12:34:56"
}
```

**Error Example:**
```json
{
  "success": false,
  "message": "Invalid credentials",
  "errorCode": "AUTHENTICATION_FAILED",
  "timestamp": "2025-01-30T12:34:56"
}
```

### 2. **Global Exception Handling**

`GlobalExceptionHandler` (@RestControllerAdvice) maps exceptions to `ApiResponse`:

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| `AuthenticationException` | 401 | AUTHENTICATION_FAILED |
| `BadCredentialsException` | 401 | AUTHENTICATION_FAILED |
| `AccessDeniedException` | 403 | ACCESS_DENIED |
| `BusinessException` | 400 | Custom |
| `ResourceNotFoundException` | 404 | RESOURCE_NOT_FOUND |
| `MethodArgumentNotValidException` | 400 | VALIDATION_ERROR |
| `Exception` (catchall) | 500 | INTERNAL_SERVER_ERROR |

### 3. **Event System**

Spring's `ApplicationEventPublisher` for inter-domain communication:

**Event Contracts:**
```java
// brokers → orders
record NewOrdersEvent(String userId, List<OrderPayload> orders);

// orders → capitalgains
record UserOrdersChangedEvent(String userId, List<Integer> taxYears);

// capitalgains → pit
record CapitalGainsReportUpdatedEvent(String userId, int taxYear);
```

**Pattern:**
```java
@Component
public class SomeDomainConsumer {
    private final ApplicationEventPublisher publisher;
    
    @EventListener
    public void on(SomeEvent event) {
        // Process event
        // ...
        // Publish next event
        publisher.publishEvent(new NextEvent(...));
    }
}
```

### 4. **@AuthUserId Annotation**

Custom argument resolver for extracting authenticated user ID:

```java
public class AuthUserIdResolver implements HandlerMethodArgumentResolver {
    @Override
    public Object resolveArgument(...) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("sub");  // Prod: JWT claim
        }
        return webRequest.getHeader("X-User-Id");  // Dev: HTTP header
    }
}
```

**Usage:**
```java
@GetMapping("/api/orders")
public ResponseEntity<ApiResponse<List<Order>>> getOrders(@AuthUserId String userId) {
    // userId automatically resolved from JWT or header
}
```

---

## Database Schema

### Tables

**1. `coinbase_token`**
```sql
CREATE TABLE coinbase_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
    expires_in INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```
Purpose: Store Coinbase OAuth tokens per user

**2. `orders`**
```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    external_id VARCHAR(255) NOT NULL,  -- Broker's order ID
    product_id VARCHAR(255) NOT NULL,   -- e.g., "BTC-USD"
    side VARCHAR(32) NOT NULL,          -- "BUY" or "SELL"
    status VARCHAR(64) NOT NULL,        -- "FILLED", "OPEN", etc.
    occurred_at TIMESTAMP NOT NULL,     -- Transaction time
    quantity DECIMAL(38, 18),           -- Crypto amount
    price DECIMAL(38, 18),              -- Unit price
    fee DECIMAL(38, 18),                -- Transaction fee
    total DECIMAL(38, 18),              -- Total value
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_orders_user_external UNIQUE (user_id, external_id)
);
```
Purpose: Broker-agnostic normalized orders

**3. `capital_gains_report`**
```sql
CREATE TABLE capital_gains_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    tax_year INT NOT NULL,
    cost DECIMAL(38, 18) NOT NULL,      -- Total purchase cost
    proceeds DECIMAL(38, 18) NOT NULL,  -- Total sales revenue
    gain DECIMAL(38, 18) NOT NULL,      -- proceeds - cost
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_capital_gains_report_user_year UNIQUE (user_id, tax_year)
);
```
Purpose: Cached capital gains calculations (USD)

**4. `pit_report`**
```sql
CREATE TABLE pit_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    tax_year INT NOT NULL,
    cost DECIMAL(38, 18) NOT NULL,      -- In PLN
    proceeds DECIMAL(38, 18) NOT NULL,  -- In PLN
    gain DECIMAL(38, 18) NOT NULL,      -- In PLN
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pit_report_user_year UNIQUE (user_id, tax_year)
);
```
Purpose: Polish tax reports with PLN conversion

### Flyway Migrations

Located in `src/main/resources/db/migration/`:
- `V1__Create_coinbase_token_table.sql`
- `V2__Create_orders_table.sql`
- `V3__Create_pit_report_table.sql`
- `V4__Create_capital_gains_report_table.sql`

---

## Configuration & Deployment

### Profiles

**1. Dev Profile (`application-dev.properties`)**
```properties
spring.profiles.active=dev

# Database
spring.datasource.url=jdbc:postgresql://db:5432/taxool-db
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update

# Logging
logging.level.com.mateuszcer.taxbackend=DEBUG

# Coinbase Mock Server
coinbase.base-url=http://mock-coinbase-server:9999
coinbase.oauth-url=http://localhost:9999

# Frontend
frontend.base-url=http://localhost:5173

# CORS
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

**2. Prod Profile (`application-prod.properties`)**
```properties
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.mateuszcer.taxbackend=WARN

# Real Coinbase
coinbase.client.id=${COINBASE_CLIENT_ID}
coinbase.client.secret=${COINBASE_CLIENT_SECRET}
coinbase.base-url=https://api.coinbase.com
coinbase.oauth-url=https://login.coinbase.com

# Production frontend
frontend.base-url=https://www.taxool.com

# NBP API (Polish National Bank)
# Configured in NbpExchangeRateClient
```

### Docker Setup

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml:**
```yaml
services:
  db:
    image: postgres:latest
    environment:
      POSTGRES_DB: taxool-db
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    networks:
      - tax-network

  spring-boot-app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - db
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    networks:
      - tax-network

networks:
  tax-network:
    name: tax-network
    driver: bridge
```

### CI/CD (GitHub Actions)

**`.github/workflows/deploy.yml`:**
```yaml
on:
  push:
    branches: [main]

jobs:
  build-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v4
        with:
          distribution: 'corretto'
          java-version: '21'
      
      - name: Build
        run: ./gradlew build
      
      - name: Docker Build & Push
        uses: docker/build-push-action@v3
        with:
          push: true
          tags: mateuszcer/tax-backend-app:${{ github.sha }}
      
      - name: Deploy to EC2
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.EC2_HOST }}
          script: |
            docker stop spring-service || true
            docker pull mateuszcer/tax-backend-app:${{ github.sha }}
            docker run -d --name spring-service -p 80:8080 \
              mateuszcer/tax-backend-app:${{ github.sha }}
```

**Deployment Target:** AWS EC2 instance

---

## API Documentation

### Swagger UI

- **URL:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api-docs`

### Configuration (`OpenApiConfig`)

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Tax Backend API")
            .version("0.1.0")
            .description("API for crypto tax automation"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(new Components()
            .addSecuritySchemes("bearerAuth", 
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
}
```

### API Endpoints Summary

| Domain | Endpoint | Method | Description |
|--------|----------|--------|-------------|
| **Auth** | `/auth/signUp` | POST | Register user |
| | `/auth/confirm` | POST | Confirm email |
| | `/auth/signIn` | POST | Sign in (get tokens) |
| **Broker** | `/api/broker/{id}/auth/url` | GET | Get OAuth URL |
| | `/api/broker/{id}/auth/callback` | GET | OAuth callback |
| | `/api/broker/{id}/orders/sync` | POST | Sync orders |
| | `/api/broker/{id}/orders` | GET | Get broker orders |
| **Orders** | `/api/orders` | GET | Get user orders |
| **PIT** | `/api/pit/{year}/preview` | GET | PIT preview (PLN) |
| | `/api/pit/{year}/generate` | POST | Generate PIT report |

---

## Design Patterns & Principles

### DDD Patterns Applied

1. **Facade Pattern**
   - Each domain has a Facade that routes Actions/Queries to Use Cases
   - Example: `BrokerFacade`, `OrderFacade`, `CapitalGainsFacade`

2. **Ports & Adapters (Hexagonal Architecture)**
   - Domain defines Ports (interfaces)
   - Infrastructure provides Adapters (implementations)
   - Example: `ExchangeRateProvider` (port) ← `NbpExchangeRateClient` (adapter)

3. **CQRS (Command Query Responsibility Segregation)**
   - Separate Actions (commands) and Queries
   - Example: `SaveNewOrdersAction`, `GetUserOrdersQuery`

4. **Aggregate Roots**
   - `Order` is an aggregate with identity (`id`) and lifecycle
   - Enforces business invariants (e.g., unique `externalId` per user)

5. **Domain Events**
   - Events for inter-domain communication
   - Loose coupling between domains
   - Example: `NewOrdersEvent`, `UserOrdersChangedEvent`

### SOLID Principles

**Single Responsibility:**
- Each Use Case does ONE thing
- Example: `CalculateCapitalGainsPreview` only calculates, doesn't persist

**Open/Closed:**
- Easy to add new brokers without modifying existing code
- Just implement `BrokerAdapter` and register in config

**Liskov Substitution:**
- `DevAuthService`, `CognitoAuthService` interchangeable via `AuthService` interface

**Interface Segregation:**
- Specific ports: `ExchangeRateProvider`, `OrderStore`, `UserOrdersProvider`
- Clients depend only on needed methods

**Dependency Inversion:**
- Domain depends on abstractions (ports)
- Infrastructure depends on domain (implements ports)

---

## Key Architectural Decisions

### 1. **Event-Driven Architecture**

**Why:** Loose coupling between domains, easy to add new consumers.

**Example:** Adding a new tax jurisdiction (e.g., Germany) would just consume `CapitalGainsReportUpdatedEvent` without touching other domains.

### 2. **Separate Capital Gains & PIT**

**Why:** Capital gains calculations are country-agnostic (FIFO), PIT is country-specific (PLN conversion).

**Benefit:** Can reuse `capitalgains` for multiple countries.

### 3. **Profile-Based Authentication**

**Why:** Dev mode needs fast iteration without AWS setup.

**Benefit:** Developers can test entire flow locally without real AWS Cognito.

### 4. **Currency Conversion in PIT Domain**

**Why:** NBP rates are PL-specific, not needed for other countries.

**Benefit:** Clean separation - capital gains stays in USD, PIT converts to PLN.

### 5. **Idempotent Order Processing**

**Why:** Broker API might return same orders multiple times during sync.

**Benefit:** Database constraint `UNIQUE(user_id, external_id)` prevents duplicates automatically.

---

## Testing Strategy

### Unit Tests

- Domain logic (Use Cases) with mocked ports
- Example: `CalculateCapitalGainsPreviewTest`

### Integration Tests

- Repository tests with Testcontainers (PostgreSQL)
- Example: `OrderRepositoryTest`, `CapitalGainsReportRepositoryTest`

### Test Profile

- `TestSecurityConfig` - All endpoints `permitAll()`
- `TestAuthService` - Mock authentication
- H2 in-memory database for fast tests

---

## Future Extensibility

### Adding a New Broker

1. **Domain:**
   - Add to `Broker` enum (e.g., `BINANCE`)
   
2. **Infrastructure:**
   - Implement `BrokerAdapter`
   - Implement `BrokerClient` (OAuth)
   - Create HTTP client for broker API
   
3. **Wiring:**
   - Register beans in config
   - Add to `BrokerFacade` map

4. **Configuration:**
   - Add broker credentials to `application-*.properties`

**No changes needed in:**
- Orders domain
- Capital Gains domain
- PIT domain
- Event system

### Adding a New Country

1. Create new domain (e.g., `germany/`)
2. Implement tax-specific logic
3. Consume `CapitalGainsReportUpdatedEvent`
4. Apply country-specific rules (e.g., German FIFO, EUR conversion)

**Reuse:** All of capital gains calculations!

---

## Monitoring & Observability

### Spring Boot Actuator

**Endpoints:**
- `/actuator/health` - Health check (public)
- `/actuator/info` - App info
- `/actuator/metrics` - Metrics
- Dev: All endpoints exposed
- Prod: Limited endpoints (`health`, `info`, `metrics`)

### Logging

**Pattern:**
```
%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

**Log Levels:**
- Dev: `DEBUG` for `com.mateuszcer.taxbackend`
- Prod: `WARN` for `com.mateuszcer.taxbackend`

**Custom Logging:**
- Dev mode auth logs with 🔓 emoji
- Request/Response logging via `LoggingInterceptor`

---

## Security Considerations

### Production Security

1. **JWT Validation:** AWS Cognito validates all tokens
2. **HTTPS:** Enforced via HSTS headers
3. **CORS:** Configured allowed origins only
4. **SQL Injection:** Prevented by JPA parameterized queries
5. **XSS:** Content-Type headers, X-XSS-Protection

### Dev Mode Warnings

- ⚠️ **Never use dev profile in production**
- ⚠️ Mock authentication accepts any password
- ⚠️ No real token validation
- ⚠️ CORS allows localhost origins

---

## Performance Optimizations

1. **Database Indexes:**
   - `idx_orders_user_id_occurred_at` for efficient time-range queries
   - Unique constraints for deduplication

2. **Caching:**
   - NBP exchange rates cached (1 hour TTL)
   - Caffeine in-memory cache

3. **Event Processing:**
   - Async processing via Spring events
   - Non-blocking for user requests

4. **Connection Pooling:**
   - HikariCP (default Spring Boot)

---

## Dependencies

**Key Dependencies:**
```gradle
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    
    // AWS
    implementation 'software.amazon.awssdk:cognitoidentityprovider:2.28.16'
    
    // Database
    implementation 'org.postgresql:postgresql:42.7.2'
    
    // API Docs
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    
    // Testing
    testImplementation 'org.testcontainers:postgresql'
}
```

---

## Conclusion

Taxool Backend is a **well-architected, maintainable, and extensible** system built on solid DDD principles. Key strengths:

✅ **Clean Architecture** - Clear separation of concerns
✅ **Event-Driven** - Loose coupling between domains
✅ **Testable** - Easy to test with mocks and Testcontainers
✅ **Extensible** - Easy to add new brokers/countries
✅ **Production-Ready** - Proper security, monitoring, CI/CD

The architecture supports the core business flow seamlessly:
**Broker Integration → Order Normalization → Capital Gains → PIT (PLN)**

Each domain is independent, well-tested, and follows SOLID principles, making future maintenance and feature additions straightforward.
