package com.mateuszcer.taxbackend.pit.domain.service;

import com.mateuszcer.taxbackend.pit.domain.port.ExchangeRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Domain service responsible for currency conversions.
 * Encapsulates the business logic of converting amounts to PLN for tax calculations.
 */
public class CurrencyConversionService {
    
    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionService.class);
    private static final String PLN = "PLN";
    
    private final ExchangeRateProvider exchangeRateProvider;
    
    public CurrencyConversionService(ExchangeRateProvider exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
    }
    
    /**
     * Convert amount from source currency to PLN at a specific date.
     * For Polish tax purposes, uses NBP (National Bank of Poland) official rates.
     * 
     * @param amount Amount in source currency
     * @param sourceCurrency ISO 4217 currency code (e.g., "USD")
     * @param transactionDate Date of transaction
     * @return Amount in PLN, or original amount if already in PLN or conversion not available
     */
    public BigDecimal convertToPln(BigDecimal amount, String sourceCurrency, LocalDate transactionDate) {
        if (amount == null || amount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        
        // Already in PLN, no conversion needed
        if (PLN.equalsIgnoreCase(sourceCurrency)) {
            return amount;
        }
        
        // Check if provider supports this currency
        if (!exchangeRateProvider.supports(sourceCurrency)) {
            log.warn("Currency {} not supported by exchange rate provider, returning original amount", sourceCurrency);
            return amount;
        }
        
        // Get exchange rate for transaction date
        Optional<BigDecimal> rate = exchangeRateProvider.getRate(sourceCurrency, transactionDate);
        
        if (rate.isEmpty()) {
            log.warn("Exchange rate not available for {} on {}, returning original amount", 
                    sourceCurrency, transactionDate);
            return amount;
        }
        
        // Convert: amount_usd * rate_usd_to_pln = amount_pln
        BigDecimal plnAmount = amount.multiply(rate.get())
                .setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Converted {} {} to {} PLN (rate: {}, date: {})", 
                amount, sourceCurrency, plnAmount, rate.get(), transactionDate);
        
        return plnAmount;
    }
}
