package com.mateuszcer.taxbackend.capitalgains.application;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsFacade;
import com.mateuszcer.taxbackend.capitalgains.domain.port.CapitalGainsReportStore;
import com.mateuszcer.taxbackend.capitalgains.domain.port.UserOrdersProvider;
import com.mateuszcer.taxbackend.capitalgains.domain.usecase.CalculateCapitalGainsPreview;
import com.mateuszcer.taxbackend.capitalgains.domain.usecase.GenerateCapitalGainsReport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CapitalGainsWiringConfig {

    @Bean
    public CalculateCapitalGainsPreview calculateCapitalGainsPreview(UserOrdersProvider userOrdersProvider) {
        return new CalculateCapitalGainsPreview(userOrdersProvider);
    }

    @Bean
    public GenerateCapitalGainsReport generateCapitalGainsReport(
            CalculateCapitalGainsPreview calculateCapitalGainsPreview,
            CapitalGainsReportStore reportStore
    ) {
        return new GenerateCapitalGainsReport(calculateCapitalGainsPreview, reportStore);
    }

    @Bean
    public CapitalGainsFacade capitalGainsFacade(
            CalculateCapitalGainsPreview calculateCapitalGainsPreview,
            GenerateCapitalGainsReport generateCapitalGainsReport
    ) {
        return new CapitalGainsFacade(calculateCapitalGainsPreview, generateCapitalGainsReport);
    }
}


