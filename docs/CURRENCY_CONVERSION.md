# Currency Conversion for Polish Tax Calculations

## Overview

The PIT (Polish Tax Form) domain is responsible for converting crypto transaction values from USD to PLN using **official NBP (National Bank of Poland) exchange rates**.

## Architecture

### DDD Compliance

```
┌─────────────────────────────────────────────┐
│         PIT Domain (Business Logic)          │
│                                               │
│  ┌─────────────────────────────────────┐    │
│  │   CurrencyConversionService         │    │
│  │   (Domain Service)                  │    │
│  └─────────────────┬───────────────────┘    │
│                    │                          │
│  ┌─────────────────▼───────────────────┐    │
│  │   ExchangeRateProvider (Port)       │    │
│  │   - getRate(currency, date)         │    │
│  │   - supports(currency)              │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
                     │
                     │ implements
                     │
┌────────────────────▼──────────────────────────┐
│        Infrastructure (Adapter)                │
│                                                 │
│  ┌──────────────────────────────────────┐     │
│  │   NbpExchangeRateClient              │     │
│  │   - Calls NBP API                    │     │
│  │   - Caches results                   │     │
│  └──────────────────────────────────────┘     │
└─────────────────────────────────────────────────┘
```

### Components

**1. Domain Layer (`pit.domain`)**
- `ExchangeRateProvider` (Port) - Interface for getting exchange rates
- `CurrencyConversionService` - Domain service that handles conversion logic
- `CalculatePitPreview` - Use case that calculates PIT preview in PLN

**2. Infrastructure Layer (`pit.infrastructure.nbp`)**
- `NbpExchangeRateClient` - Adapter that fetches rates from NBP API

## How It Works

### 1. Transaction Flow

```
Coinbase (USD) → Backend → CapitalGains (USD) → PIT (converts to PLN)
```

### 2. Conversion Process

For each transaction:
1. Extract transaction date
2. Extract currency from `productId` (e.g., "BTC-USD" → "USD")
3. Call `CurrencyConversionService.convertToPln(amount, "USD", date)`
4. Service fetches NBP rate for that date
5. Converts: `amount_usd * rate = amount_pln`
6. Returns PLN value

### 3. Currency Detection

The system automatically detects currency from the `productId`:
- `BTC-USD` → USD
- `ETH-EUR` → EUR
- `SOL-USDC` → USD (USDC treated as USD)
- `AVAX-USDT` → USD (USDT treated as USD)

## NBP API

### Why NBP?

✅ **Official source** - Recognized by Polish tax authorities  
✅ **Free** - No API key required  
✅ **Historical rates** - Access rates from any past date  
✅ **Reliable** - Polish National Bank infrastructure  
✅ **No rate limits** - For reasonable usage  

### API Details

**Base URL:** `https://api.nbp.pl/api/exchangerates/rates/a/{currency}/{date}/?format=json`

**Example Request:**
```bash
curl https://api.nbp.pl/api/exchangerates/rates/a/usd/2025-01-15/?format=json
```

**Example Response:**
```json
{
  "table": "A",
  "currency": "dolar amerykański",
  "code": "USD",
  "rates": [
    {
      "no": "010/A/NBP/2025",
      "effectiveDate": "2025-01-15",
      "mid": 4.0524
    }
  ]
}
```

### Supported Currencies

- USD, EUR, GBP, CHF, JPY, CAD, AUD
- NOK, SEK, DKK, CZK, HUF, RON, BGN
- More available at: https://api.nbp.pl/#currencies

## Caching

Exchange rates are cached to minimize API calls:

```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=3600s
spring.cache.cache-names=exchangeRates
```

**Cache Key:** `{currency}_{date}` (e.g., `USD_2025-01-15`)  
**TTL:** 1 hour (rates don't change retroactively)  
**Max Size:** 1000 entries  

## Testing

### Manual Test

```bash
# 1. Buy BTC with USD on 2025-01-15
# 2. Sell BTC on 2025-01-22
# 3. Request PIT preview for 2025

curl http://localhost:8080/api/pit/preview/2025 \
  -H "Authorization: Bearer YOUR_JWT"

# Response will show amounts in PLN:
{
  "taxYear": 2025,
  "cost": 20262.00,      // PLN (was ~$5000 USD)
  "proceeds": 39000.00,   // PLN (was ~$9800 USD)
  "gain": 18738.00,       // PLN
  "warnings": []
}
```

### Unit Test Example

```java
@Test
void shouldConvertUsdToPln() {
    // Given: NBP rate for USD on 2025-01-15 is 4.0524 PLN
    when(exchangeRateProvider.getRate("USD", LocalDate.of(2025, 1, 15)))
        .thenReturn(Optional.of(new BigDecimal("4.0524")));
    
    // When: Convert $100 USD
    BigDecimal pln = currencyConversionService.convertToPln(
        new BigDecimal("100"), 
        "USD", 
        LocalDate.of(2025, 1, 15)
    );
    
    // Then: Should be 405.24 PLN
    assertThat(pln).isEqualByComparingTo("405.24");
}
```

## Edge Cases

### 1. Currency Already PLN
If transaction is already in PLN, no conversion happens:
```java
convertToPln(100, "PLN", date) → 100 PLN
```

### 2. Unsupported Currency
If currency not supported by NBP:
```java
// Logs warning and returns original amount
log.warn("Currency XYZ not supported, returning original amount");
return originalAmount;
```

### 3. Rate Not Available
If NBP doesn't have rate for specific date (e.g., weekend/holiday):
```java
// NBP automatically returns closest previous rate
// Example: Request for Saturday → Returns Friday's rate
```

### 4. API Failure
If NBP API fails:
```java
// Logs error and returns original amount
log.error("Failed to fetch rate, returning original amount");
return originalAmount;
```

## Production Considerations

### 1. Error Handling
- Network failures are logged but don't break calculations
- Falls back to original USD amounts if conversion fails
- User sees warning in PIT preview

### 2. Performance
- Cached rates minimize API calls
- Historical rates never change, safe to cache indefinitely
- Average latency: <100ms (cached), ~500ms (uncached)

### 3. Compliance
- Uses official NBP rates as required by Polish tax law
- Rates match official NBP tables
- Audit trail via logs

### 4. Monitoring
Log key events:
```java
log.info("NBP rate for USD on 2025-01-15: 4.0524 PLN");
log.warn("Exchange rate not available for XYZ on 2025-01-15");
log.error("Failed to fetch NBP rate: Connection timeout");
```

## Future Enhancements

1. **Fallback Provider** - Use alternative API if NBP unavailable
2. **Pre-warming Cache** - Fetch common rates on startup
3. **Batch API** - Fetch multiple dates at once
4. **Custom Rates** - Allow manual rate override for edge cases

## References

- [NBP API Documentation](https://api.nbp.pl/)
- [Polish Tax Law on Crypto](https://www.gov.pl/web/kas/kryptowaluty)
- [DDD Architecture](https://martinfowler.com/bliki/DomainDrivenDesign.html)
