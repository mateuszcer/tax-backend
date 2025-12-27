package com.mateuszcer.taxbackend.pit.application;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsFacade;
import com.mateuszcer.taxbackend.pit.domain.PitFacade;
import com.mateuszcer.taxbackend.pit.domain.port.PitReportStore;
import com.mateuszcer.taxbackend.pit.domain.usecase.CalculatePitPreview;
import com.mateuszcer.taxbackend.pit.domain.usecase.GeneratePitReport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PitWiringConfig {

    @Bean
    public CalculatePitPreview calculatePitPreview(CapitalGainsFacade capitalGainsFacade) {
        return new CalculatePitPreview(capitalGainsFacade);
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


