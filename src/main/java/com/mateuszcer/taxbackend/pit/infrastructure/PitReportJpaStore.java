package com.mateuszcer.taxbackend.pit.infrastructure;

import com.mateuszcer.taxbackend.pit.domain.PitReport;
import com.mateuszcer.taxbackend.pit.domain.port.PitReportStore;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PitReportJpaStore implements PitReportStore {

    private final PitReportRepository repository;

    public PitReportJpaStore(PitReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PitReport> findByUserIdAndTaxYear(String userId, int taxYear) {
        return repository.findByUserIdAndTaxYear(userId, taxYear);
    }

    @Override
    public PitReport save(PitReport report) {
        return repository.save(report);
    }
}


