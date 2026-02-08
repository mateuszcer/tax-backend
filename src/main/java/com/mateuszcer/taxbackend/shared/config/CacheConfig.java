package com.mateuszcer.taxbackend.shared.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enable Spring Boot caching for market data and exchange rates.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Caching is now enabled!
    // Configuration is in application-*.properties
}
