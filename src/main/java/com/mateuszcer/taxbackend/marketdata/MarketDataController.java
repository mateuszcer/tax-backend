package com.mateuszcer.taxbackend.marketdata;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple market data proxy endpoint - caches CoinGecko data to avoid rate limiting.
 * No authentication required (public data).
 */
@RestController
@RequestMapping("/api/market")
@Tag(name = "Market Data", description = "Cryptocurrency market data (cached)")
@Slf4j
public class MarketDataController {
    
    private final CoinGeckoService coinGeckoService;
    
    public MarketDataController(CoinGeckoService coinGeckoService) {
        this.coinGeckoService = coinGeckoService;
    }
    
    @GetMapping("/coins")
    @Operation(
        summary = "Get cryptocurrency market data", 
        description = "Returns top cryptocurrencies by market cap. Cached for 10 minutes to avoid rate limiting."
    )
    public ResponseEntity<String> getCoins(
            @RequestParam(defaultValue = "usd") String vsCurrency,
            @RequestParam(defaultValue = "100") int perPage,
            @RequestParam(defaultValue = "1") int page
    ) {
        log.debug("Fetching market data: vsCurrency={}, perPage={}, page={}", vsCurrency, perPage, page);
        
        String data = coinGeckoService.getMarketData(vsCurrency, perPage, page);
        
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=600") // 10 minutes
                .body(data);
    }
}
