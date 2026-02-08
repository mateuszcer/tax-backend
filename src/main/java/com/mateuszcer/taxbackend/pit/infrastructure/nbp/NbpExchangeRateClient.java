package com.mateuszcer.taxbackend.pit.infrastructure.nbp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mateuszcer.taxbackend.pit.domain.port.ExchangeRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

@Service
public class NbpExchangeRateClient implements ExchangeRateProvider {
    
    private static final Logger log = LoggerFactory.getLogger(NbpExchangeRateClient.class);
    private static final String NBP_API_URL = "https://api.nbp.pl/api/exchangerates/rates/a";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "USD", "EUR", "GBP", "CHF", "JPY", "CAD", "AUD", 
            "NOK", "SEK", "DKK", "CZK", "HUF", "RON", "BGN"
    );
    
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    
    public NbpExchangeRateClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Cacheable(value = "exchangeRates", key = "#currencyCode + '_' + #date")
    public Optional<BigDecimal> getRate(String currencyCode, LocalDate date) {
        // Try the requested date first, then fallback to previous days (up to 7 days)
        // This handles weekends and Polish holidays when NBP doesn't publish rates
        for (int daysBack = 0; daysBack <= 7; daysBack++) {
            LocalDate tryDate = date.minusDays(daysBack);
            
            try {
                String url = String.format("%s/%s/%s/?format=json", 
                        NBP_API_URL, 
                        currencyCode.toLowerCase(), 
                        tryDate.format(DATE_FORMATTER));
                
                if (daysBack == 0) {
                    log.debug("Fetching NBP exchange rate for {} on {}", currencyCode, tryDate);
                } else {
                    log.debug("Trying fallback date {} for {} (original date: {})", 
                            tryDate, currencyCode, date);
                }
                
                String response = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);
                
                if (response == null) {
                    continue; // Try previous day
                }
                
                // Parse JSON response
                JsonNode root = objectMapper.readTree(response);
                JsonNode rates = root.path("rates");
                
                if (!rates.isArray() || rates.size() == 0) {
                    continue; // Try previous day
                }
                
                JsonNode firstRate = rates.get(0);
                BigDecimal mid = firstRate.path("mid").decimalValue();
                
                if (mid != null && mid.signum() > 0) {
                    if (daysBack > 0) {
                        log.info("NBP rate for {} on {} not available (weekend/holiday), using rate from {}: {} PLN", 
                                currencyCode, date, tryDate, mid);
                    } else {
                        log.info("NBP rate for {} on {}: {} PLN", currencyCode, date, mid);
                    }
                    return Optional.of(mid);
                }
                
            } catch (Exception e) {
                // If it's a 404, it's likely a weekend/holiday, try previous day
                if (daysBack == 7) {
                    // Only log error on the last attempt
                    log.error("Failed to fetch NBP exchange rate for {} on {} (tried {} days back): {}", 
                            currencyCode, date, daysBack, e.getMessage());
                }
                // Continue to next day
            }
        }
        
        log.warn("Could not find NBP exchange rate for {} around date {} (tried 7 days back)", 
                currencyCode, date);
        return Optional.empty();
    }
    
    @Override
    public boolean supports(String currencyCode) {
        return SUPPORTED_CURRENCIES.contains(currencyCode.toUpperCase());
    }
}
