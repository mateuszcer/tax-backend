## Tax Backend

Backend do automatyzacji rozliczania podatku od zysków kapitałowych dla inwestorów krypto (PIT w PL). Integruje brokerów (np. Coinbase), normalizuje zlecenia do wspólnego modelu i generuje raporty (capital gains + PIT) w przepływie zdarzeniowym.

## Słownik pojęć (krótko)

- **Broker**: zewnętrzny dostawca danych o zleceniach użytkownika (Coinbase, w przyszłości kolejne).
- **Order**: znormalizowane zlecenie użytkownika w naszym systemie (broker-agnostic).
- **Capital Gains**: wyliczenia koszt/proceeds/gain (logika wspólna, niezależna od kraju).
- **PIT**: adapter kraju (na dziś PL) bazujący na danych z `capitalgains`.

## Architektura (DDD + warstwy)

Wszystkie domeny trzymają się spójnego schematu:

- **API/Consumer**: kontrolery REST lub konsumenci zdarzeń (Spring `@RestController` / `@EventListener`)
- **Facade**: router akcji/zapytań do pojedynczych usecase’ów
- **Usecase**: jedna odpowiedzialność (1 czynność biznesowa)
- **Infra**: integracje (HTTP, DB), implementacje portów

W praktyce w kodzie oznacza to paczki:

- `.../application`: kontrolery, wiring, konsumenci eventów
- `.../domain`: facady, usecase’y, akcje/zapytania, porty, model
- `.../infrastructure`: adaptery do DB/HTTP, repozytoria JPA

## Domeny i odpowiedzialności

### `brokers`

**Po co istnieje**: jeden, generyczny punkt integracji z brokerami + możliwość dodania kolejnego brokera bez dotykania pozostałych domen.

- **API**: `BrokerController` pod `/api/broker/{brokerId}/...`
- **Domena**:
  - `Broker` (enum): lista wspieranych brokerów
  - `BrokerFacade`: routuje `Action/Query -> Usecase`
  - `SyncBrokerOrders`: pobiera zlecenia z brokera i publikuje `NewOrdersEvent`
  - Porty: `BrokerAdapter`, `NewOrdersPublisher`, `OAuthClient`, `OAuthTokenStore`
- **Integracje (Coinbase)**:
  - `CoinbaseBrokerAdapter`: mapowanie odpowiedzi Coinbase do wspólnego `NewOrdersEvent.OrderPayload`
  - `CoinbaseOAuthClientAdapter`: adapter OAuth2 (token exchange)
  - `CoinbaseTokenStoreAdapter`: zapis/odczyt tokenów OAuth w DB
  - `CoinbaseClient`: HTTP client do Coinbase API

### `orders`

**Po co istnieje**: jedna prawda o zleceniach użytkownika w ustandaryzowanym formacie (niezależnie od brokera).

- **Consumer**: `NewOrdersEventConsumer` (konsumuje `NewOrdersEvent`)
- **Domena**:
  - `Order` (aggregate): model zlecenia
  - `SaveNewOrders`: zapisuje nowe zlecenia (idempotencja po `externalId`/brokerze jest po stronie infra/store)
  - `GetUserOrders`: zwraca listę zleceń
  - Port: `OrderStore`
- **Infra**: `OrderRepository` + `OrderJpaStore`
- **API**: `GET /api/orders` (zwraca `ApiResponse<List<OrderResponse>>`)
- **Publikowane zdarzenie**: `UserOrdersChangedEvent` (po aktualizacji zleceń użytkownika)

### `capitalgains`

**Po co istnieje**: wspólna logika wyliczeń (FIFO) i raportów zysków kapitałowych niezależna od PIT/PL.

- **Consumer**: `UserOrdersChangedEventConsumer` (konsumuje `UserOrdersChangedEvent`)
- **Domena**:
  - `CalculateCapitalGainsPreview`: liczy podgląd (cost/proceeds/gain) dla roku podatkowego
  - `GenerateCapitalGainsReport`: zapisuje raport w DB
  - Porty: `UserOrdersProvider` (źródło orderów), `CapitalGainsReportStore` (persistencja raportu)
- **Infra**:
  - `OrdersJpaProvider`: pobiera ordery z domeny `orders` (adapter portu)
  - `CapitalGainsReportJpaStore` + repozytorium
- **Publikowane zdarzenie**: `CapitalGainsReportUpdatedEvent` (po wygenerowaniu raportu)

### `pit`

**Po co istnieje**: adapter kraju (PL) — generowanie raportu PIT na bazie danych z `capitalgains` (PIT nie liczy FIFO od zera).

- **API**: `/api/pit/{taxYear}/preview`, `/api/pit/{taxYear}/generate`
- **Domena**:
  - `CalculatePitPreview`: wywołuje `capitalgains` i mapuje wynik do formatu PIT
  - `GeneratePitReport`: zapis raportu PIT w DB
  - Port: `PitReportStore`
- **Consumer**: `CapitalGainsReportUpdatedEventConsumer` (konsumuje `CapitalGainsReportUpdatedEvent` i aktualizuje/generuje PIT)
- **Infra**: `PitReportJpaStore` + repozytorium

### `security`

- **API**: `/auth/signUp`, `/auth/confirm`, `/auth/signIn`
- **Tokeny po signIn**: `ApiResponse.data.idToken` oraz `ApiResponse.data.accessToken`
- **Id użytkownika**: `@AuthUserId` czyta claim `sub` z JWT; w dev/test dopuszcza `X-User-Id` (ułatwia integrację i testy).
- **Profile**:
  - `dev`: mock auth (każde hasło działa)
  - `simple-auth`: JWT + PostgreSQL (bez AWS Cognito, idealne do demo/małych wdrożeń)
  - `prod`: AWS Cognito (pełna integracja chmury)

### `shared`

- `ApiResponse<T>`: standardowa koperta odpowiedzi
- `GlobalExceptionHandler`: mapowanie wyjątków na spójne błędy API
- `events/*`: kontrakty zdarzeń między domenami
- `OpenApiConfig`: konfiguracja OpenAPI/Swagger

## Kontrakty zdarzeń (event-driven)

- **`NewOrdersEvent`** (`brokers` -> `orders`)
  - payload: lista znormalizowanych orderów + broker + userId
- **`UserOrdersChangedEvent`** (`orders` -> `capitalgains`)
  - sygnał: „zlecenia użytkownika się zmieniły”
- **`CapitalGainsReportUpdatedEvent`** (`capitalgains` -> `pit`)
  - sygnał: „zaktualizowano raport capital gains”

## Główny flow biznesowy (od integracji do PIT)

1. **Użytkownik integruje brokera**:
   - FE kieruje użytkownika na OAuth (`/api/broker/{brokerId}/auth` lub pobiera URL z `/api/broker/{brokerId}/auth/url`)
   - callback `/api/broker/{brokerId}/auth/callback?code=...` zapisuje tokeny OAuth w store
2. **Użytkownik uruchamia synchronizację**:
   - `POST /api/broker/{brokerId}/orders/sync`
   - `brokers` pobiera zlecenia od brokera i publikuje `NewOrdersEvent`
3. **Normalizacja i zapis orderów**:
   - `orders` konsumuje `NewOrdersEvent`, zapisuje `Order` w DB i publikuje `UserOrdersChangedEvent`
4. **Wyliczenie zysków kapitałowych**:
   - `capitalgains` konsumuje `UserOrdersChangedEvent`, liczy FIFO, zapisuje raport i publikuje `CapitalGainsReportUpdatedEvent`
5. **Wygenerowanie/aktualizacja PIT**:
   - `pit` konsumuje `CapitalGainsReportUpdatedEvent` i zapisuje/aktualizuje `pit_report`

## Jak dodać nowego brokera (checklista)

- **Domena**:
  - dopisz wartość w `Broker` (enum)
  - dodaj implementację `BrokerAdapter` (mapowanie zewnętrznych danych do `NewOrdersEvent.OrderPayload`)
- **OAuth/tokeny (jeśli dotyczy)**:
  - adapter `OAuthClient` (token exchange)
  - adapter `OAuthTokenStore` (persistencja tokenów)
- **Wiring**:
  - dopnij beany w configu domeny brokera (na wzór `CoinbaseWiringConfig`)

## Model danych (high-level)

- `orders`: znormalizowane ordery użytkownika
- `capital_gains_report`: raport capital gains (np. per user/rok)
- `pit_report`: raport PIT (per user/rok)
- `coinbase_token`: tokeny OAuth dla Coinbase (adapter brokera)

## OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Uruchomienie lokalne (skrót)

### Dev mode (mock auth):
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Simple auth (JWT + PostgreSQL, bez Cognito):
```bash
# Z docker-compose (już skonfigurowany)
docker-compose up --build

# Lub z gradlew (wymaga lokalnego PostgreSQL)
./gradlew bootRun --args='--spring.profiles.active=simple-auth'
```

### Testy:
```bash
./gradlew test
```

### Szybki start dla demo:
Zobacz **QUICK_START.md** - wdrożenie w 15 minut bez kosztów cloud!
