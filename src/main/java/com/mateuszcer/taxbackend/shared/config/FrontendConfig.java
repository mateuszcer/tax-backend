package com.mateuszcer.taxbackend.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "frontend")
@Getter
@Setter
public class FrontendConfig {
    
    /**
     * Base URL of the frontend application.
     * Dev: http://localhost:3000
     * Prod: https://www.taxool.com
     */
    private String baseUrl;
}
