package com.mateuszcer.taxbackend.pit.domain.usecase;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsPreview;
import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsFacade;
import com.mateuszcer.taxbackend.capitalgains.domain.query.CapitalGainsPreviewQuery;
import com.mateuszcer.taxbackend.pit.domain.PitPreview;
import com.mateuszcer.taxbackend.pit.domain.query.PitPreviewQuery;

public class CalculatePitPreview {

    private final CapitalGainsFacade capitalGainsFacade;

    public CalculatePitPreview(CapitalGainsFacade capitalGainsFacade) {
        this.capitalGainsFacade = capitalGainsFacade;
    }

    public PitPreview execute(PitPreviewQuery query) {
        CapitalGainsPreview preview = capitalGainsFacade.handle(new CapitalGainsPreviewQuery(query.userId(), query.taxYear()));

        return new PitPreview(
                preview.taxYear(),
                preview.cost(),
                preview.proceeds(),
                preview.gain(),
                preview.warnings()
        );
    }
}

