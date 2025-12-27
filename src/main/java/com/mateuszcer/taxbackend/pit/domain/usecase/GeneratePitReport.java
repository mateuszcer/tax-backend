package com.mateuszcer.taxbackend.pit.domain.usecase;

import com.mateuszcer.taxbackend.pit.domain.PitPreview;
import com.mateuszcer.taxbackend.pit.domain.PitReport;
import com.mateuszcer.taxbackend.pit.domain.action.GeneratePitReportAction;
import com.mateuszcer.taxbackend.pit.domain.port.PitReportStore;
import com.mateuszcer.taxbackend.pit.domain.query.PitPreviewQuery;

import java.math.BigDecimal;

public class GeneratePitReport {

    private final CalculatePitPreview calculatePitPreview;
    private final PitReportStore pitReportStore;

    public GeneratePitReport(CalculatePitPreview calculatePitPreview, PitReportStore pitReportStore) {
        this.calculatePitPreview = calculatePitPreview;
        this.pitReportStore = pitReportStore;
    }

    public PitReport execute(GeneratePitReportAction action) {
        PitPreview preview = calculatePitPreview.execute(new PitPreviewQuery(action.userId(), action.taxYear()));

        PitReport report = pitReportStore.findByUserIdAndTaxYear(action.userId(), action.taxYear())
                .orElseGet(PitReport::new);

        report.setUserId(action.userId());
        report.setTaxYear(action.taxYear());
        report.setCost(nz(preview.cost()));
        report.setProceeds(nz(preview.proceeds()));
        report.setGain(nz(preview.gain()));

        return pitReportStore.save(report);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}


