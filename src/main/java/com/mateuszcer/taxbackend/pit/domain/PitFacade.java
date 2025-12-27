package com.mateuszcer.taxbackend.pit.domain;

import com.mateuszcer.taxbackend.pit.domain.action.GeneratePitReportAction;
import com.mateuszcer.taxbackend.pit.domain.query.PitPreviewQuery;
import com.mateuszcer.taxbackend.pit.domain.usecase.CalculatePitPreview;
import com.mateuszcer.taxbackend.pit.domain.usecase.GeneratePitReport;

public class PitFacade {

    private final CalculatePitPreview calculatePitPreview;
    private final GeneratePitReport generatePitReport;

    public PitFacade(CalculatePitPreview calculatePitPreview, GeneratePitReport generatePitReport) {
        this.calculatePitPreview = calculatePitPreview;
        this.generatePitReport = generatePitReport;
    }

    public PitPreview handle(PitPreviewQuery query) {
        return calculatePitPreview.execute(query);
    }

    public PitReport handle(GeneratePitReportAction action) {
        return generatePitReport.execute(action);
    }
}


