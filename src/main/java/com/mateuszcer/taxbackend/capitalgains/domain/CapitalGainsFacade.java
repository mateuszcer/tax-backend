package com.mateuszcer.taxbackend.capitalgains.domain;

import com.mateuszcer.taxbackend.capitalgains.domain.action.GenerateCapitalGainsReportAction;
import com.mateuszcer.taxbackend.capitalgains.domain.query.CapitalGainsPreviewQuery;
import com.mateuszcer.taxbackend.capitalgains.domain.usecase.CalculateCapitalGainsPreview;
import com.mateuszcer.taxbackend.capitalgains.domain.usecase.GenerateCapitalGainsReport;

public class CapitalGainsFacade {

    private final CalculateCapitalGainsPreview calculateCapitalGainsPreview;
    private final GenerateCapitalGainsReport generateCapitalGainsReport;

    public CapitalGainsFacade(
            CalculateCapitalGainsPreview calculateCapitalGainsPreview,
            GenerateCapitalGainsReport generateCapitalGainsReport
    ) {
        this.calculateCapitalGainsPreview = calculateCapitalGainsPreview;
        this.generateCapitalGainsReport = generateCapitalGainsReport;
    }

    public CapitalGainsPreview handle(CapitalGainsPreviewQuery query) {
        return calculateCapitalGainsPreview.execute(query);
    }

    public CapitalGainsReport handle(GenerateCapitalGainsReportAction action) {
        return generateCapitalGainsReport.execute(action);
    }
}


