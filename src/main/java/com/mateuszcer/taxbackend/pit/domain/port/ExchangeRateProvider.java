package com.mateuszcer.taxbackend.pit.domain.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Port for fetching historical exchange rates.
 * For Polish tax purposes, uses official NBP (National Bank of Poland) rates.
 */
public interface ExchangeRateProvider {
    
    /**
     * Get exchange rate from source currency to PLN for a specific date.
     * 
     * @param currencyCode ISO 4217 currency code (e.g., "USD", "EUR")
     * @param date Transaction date
     * @return Exchange rate to PLN, or empty if not available
     */
    Optional<BigDecimal> getRate(String currencyCode, LocalDate date);
    
    /**
     * Check if the provider supports a given currency.
     * 
     * @param currencyCode ISO 4217 currency code
     * @return true if currency is supported
     */
    boolean supports(String currencyCode);
}
