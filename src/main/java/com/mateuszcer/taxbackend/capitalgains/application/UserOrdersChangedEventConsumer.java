package com.mateuszcer.taxbackend.capitalgains.application;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsFacade;
import com.mateuszcer.taxbackend.capitalgains.domain.action.GenerateCapitalGainsReportAction;
import com.mateuszcer.taxbackend.shared.events.CapitalGainsReportUpdatedEvent;
import com.mateuszcer.taxbackend.shared.events.UserOrdersChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserOrdersChangedEventConsumer {

    private final CapitalGainsFacade capitalGainsFacade;
    private final ApplicationEventPublisher publisher;

    public UserOrdersChangedEventConsumer(CapitalGainsFacade capitalGainsFacade, ApplicationEventPublisher publisher) {
        this.capitalGainsFacade = capitalGainsFacade;
        this.publisher = publisher;
    }

    @EventListener
    public void on(UserOrdersChangedEvent event) {
        if (event == null || event.userId() == null || event.taxYears() == null) {
            return;
        }

        List<Integer> years = event.taxYears().stream().filter(y -> y != null).distinct().sorted().toList();
        for (Integer year : years) {
            capitalGainsFacade.handle(new GenerateCapitalGainsReportAction(event.userId(), year));
            publisher.publishEvent(new CapitalGainsReportUpdatedEvent(event.userId(), year));
        }
    }
}


