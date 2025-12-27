package com.mateuszcer.taxbackend.pit.application;

import com.mateuszcer.taxbackend.pit.domain.PitFacade;
import com.mateuszcer.taxbackend.pit.domain.action.GeneratePitReportAction;
import com.mateuszcer.taxbackend.shared.events.CapitalGainsReportUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CapitalGainsReportUpdatedEventConsumer {

    private final PitFacade pitFacade;

    public CapitalGainsReportUpdatedEventConsumer(PitFacade pitFacade) {
        this.pitFacade = pitFacade;
    }

    @EventListener
    public void on(CapitalGainsReportUpdatedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        pitFacade.handle(new GeneratePitReportAction(event.userId(), event.taxYear()));
    }
}


