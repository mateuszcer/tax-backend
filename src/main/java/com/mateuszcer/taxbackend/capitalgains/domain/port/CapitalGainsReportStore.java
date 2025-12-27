package com.mateuszcer.taxbackend.capitalgains.domain.port;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsReport;

import java.util.Optional;

public interface CapitalGainsReportStore {
    Optional<CapitalGainsReport> findByUserIdAndTaxYear(String userId, int taxYear);

    CapitalGainsReport save(CapitalGainsReport report);
}


