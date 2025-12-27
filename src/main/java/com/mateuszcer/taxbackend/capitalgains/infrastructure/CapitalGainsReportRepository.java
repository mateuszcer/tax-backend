package com.mateuszcer.taxbackend.capitalgains.infrastructure;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CapitalGainsReportRepository extends JpaRepository<CapitalGainsReport, Long> {
    Optional<CapitalGainsReport> findByUserIdAndTaxYear(String userId, int taxYear);
}


