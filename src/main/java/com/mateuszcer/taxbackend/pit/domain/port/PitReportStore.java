package com.mateuszcer.taxbackend.pit.domain.port;

import com.mateuszcer.taxbackend.pit.domain.PitReport;

import java.util.Optional;

public interface PitReportStore {
    Optional<PitReport> findByUserIdAndTaxYear(String userId, int taxYear);

    PitReport save(PitReport report);
}


