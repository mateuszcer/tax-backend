package com.mateuszcer.taxbackend.pit.application;

import com.mateuszcer.taxbackend.capitalgains.domain.port.UserOrdersProvider;
import com.mateuszcer.taxbackend.pit.domain.PitFacade;
import com.mateuszcer.taxbackend.pit.domain.port.ExchangeRateProvider;
import com.mateuszcer.taxbackend.pit.domain.port.PitReportStore;
import com.mateuszcer.taxbackend.pit.domain.service.CurrencyConversionService;
import com.mateuszcer.taxbackend.pit.domain.usecase.CalculatePitPreview;
import com.mateuszcer.taxbackend.pit.domain.usecase.GeneratePitReport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PitWiringConfig {

    @Bean
    public CurrencyConversionService currencyConversionService(ExchangeRateProvider exchangeRateProvider) {
        return new CurrencyConversionService(exchangeRateProvider);
    }

    @Bean
    public CalculatePitPreview calculatePitPreview(
            UserOrdersProvider ordersProvider,
            CurrencyConversionService currencyConversionService) {
        return new CalculatePitPreview(ordersProvider, currencyConversionService);
    }

    @Bean
    public GeneratePitReport generatePitReport(CalculatePitPreview calculatePitPreview, PitReportStore pitReportStore) {
        return new GeneratePitReport(calculatePitPreview, pitReportStore);
    }

    @Bean
    public PitFacade pitFacade(CalculatePitPreview calculatePitPreview, GeneratePitReport generatePitReport) {
        return new PitFacade(calculatePitPreview, generatePitReport);
    }
}


