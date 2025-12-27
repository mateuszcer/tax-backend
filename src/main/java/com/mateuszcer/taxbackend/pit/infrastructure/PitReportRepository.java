package com.mateuszcer.taxbackend.pit.infrastructure;

import com.mateuszcer.taxbackend.pit.domain.PitReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PitReportRepository extends JpaRepository<PitReport, Long> {
    Optional<PitReport> findByUserIdAndTaxYear(String userId, int taxYear);
}


