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
        try {
            String url = String.format("%s/%s/%s/?format=json", 
                    NBP_API_URL, 
                    currencyCode.toLowerCase(), 
                    date.format(DATE_FORMATTER));
            
            log.debug("Fetching NBP exchange rate for {} on {}", currencyCode, date);
            
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            
            if (response == null) {
                log.warn("Empty response from NBP API for {} on {}", currencyCode, date);
                return Optional.empty();
            }
            
            // Parse JSON response
            JsonNode root = objectMapper.readTree(response);
            JsonNode rates = root.path("rates");
            
            if (!rates.isArray() || rates.size() == 0) {
                log.warn("No rates found in NBP response for {} on {}", currencyCode, date);
                return Optional.empty();
            }
            
            JsonNode firstRate = rates.get(0);
            BigDecimal mid = firstRate.path("mid").decimalValue();
            
            if (mid != null && mid.signum() > 0) {
                log.info("NBP rate for {} on {}: {} PLN", currencyCode, date, mid);
                return Optional.of(mid);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to fetch NBP exchange rate for {} on {}: {}", 
                    currencyCode, date, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public boolean supports(String currencyCode) {
        return SUPPORTED_CURRENCIES.contains(currencyCode.toUpperCase());
    }
}
