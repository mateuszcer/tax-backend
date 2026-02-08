package com.mateuszcer.taxbackend.marketdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service for fetching cryptocurrency market data from CoinGecko API.
 * Heavily cached to avoid rate limiting.
 */
@Service
@Slf4j
public class CoinGeckoService {
    
    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3";
    
    private final RestClient restClient;
    
    public CoinGeckoService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }
    
    /**
     * Get cryptocurrency market data from CoinGecko.
     * Cached for 10 minutes to reduce API calls and avoid rate limiting.
     * 
     * @param vsCurrency Base currency (e.g., "usd")
     * @param perPage Number of results per page
     * @param page Page number
     * @return JSON response from CoinGecko
     */
    @Cacheable(value = "marketData", key = "#vsCurrency + '_' + #perPage + '_' + #page")
    public String getMarketData(String vsCurrency, int perPage, int page) {
        String url = String.format(
                "%s/coins/markets?vs_currency=%s&order=market_cap_desc&per_page=%d&page=%d&sparkline=false&price_change_percentage=24h",
                COINGECKO_API_URL,
                vsCurrency,
                perPage,
                page
        );
        
        log.info("Fetching fresh market data from CoinGecko (will be cached for 10 minutes)");
        
        try {
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            
            log.debug("Successfully fetched market data from CoinGecko");
            return response;
            
        } catch (Exception e) {
            log.error("Failed to fetch market data from CoinGecko: {}", e.getMessage());
            // Return empty array as fallback
            return "[]";
        }
    }
}
