package com.mateuszcer.taxbackend.capitalgains.domain.usecase;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsPreview;
import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsReport;
import com.mateuszcer.taxbackend.capitalgains.domain.action.GenerateCapitalGainsReportAction;
import com.mateuszcer.taxbackend.capitalgains.domain.port.CapitalGainsReportStore;
import com.mateuszcer.taxbackend.capitalgains.domain.query.CapitalGainsPreviewQuery;

import java.math.BigDecimal;

public class GenerateCapitalGainsReport {

    private final CalculateCapitalGainsPreview calculateCapitalGainsPreview;
    private final CapitalGainsReportStore reportStore;

    public GenerateCapitalGainsReport(
            CalculateCapitalGainsPreview calculateCapitalGainsPreview,
            CapitalGainsReportStore reportStore
    ) {
        this.calculateCapitalGainsPreview = calculateCapitalGainsPreview;
        this.reportStore = reportStore;
    }

    public CapitalGainsReport execute(GenerateCapitalGainsReportAction action) {
        CapitalGainsPreview preview = calculateCapitalGainsPreview.execute(
                new CapitalGainsPreviewQuery(action.userId(), action.taxYear())
        );

        CapitalGainsReport report = reportStore.findByUserIdAndTaxYear(action.userId(), action.taxYear())
                .orElseGet(CapitalGainsReport::new);

        report.setUserId(action.userId());
        report.setTaxYear(action.taxYear());
        report.setCost(nz(preview.cost()));
        report.setProceeds(nz(preview.proceeds()));
        report.setGain(nz(preview.gain()));

        return reportStore.save(report);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}


