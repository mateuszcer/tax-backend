package com.mateuszcer.taxbackend.capitalgains.infrastructure;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsReport;
import com.mateuszcer.taxbackend.capitalgains.domain.port.CapitalGainsReportStore;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CapitalGainsReportJpaStore implements CapitalGainsReportStore {

    private final CapitalGainsReportRepository repository;

    public CapitalGainsReportJpaStore(CapitalGainsReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CapitalGainsReport> findByUserIdAndTaxYear(String userId, int taxYear) {
        return repository.findByUserIdAndTaxYear(userId, taxYear);
    }

    @Override
    public CapitalGainsReport save(CapitalGainsReport report) {
        return repository.save(report);
    }
}


