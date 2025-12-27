package com.mateuszcer.taxbackend.orders.application;

import com.mateuszcer.taxbackend.orders.domain.OrderFacade;
import com.mateuszcer.taxbackend.orders.domain.action.SaveNewOrdersAction;
import com.mateuszcer.taxbackend.shared.events.NewOrdersEvent;
import com.mateuszcer.taxbackend.shared.events.UserOrdersChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class NewOrdersEventConsumer {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final OrderFacade orderFacade;
    private final ApplicationEventPublisher publisher;

    public NewOrdersEventConsumer(OrderFacade orderFacade, ApplicationEventPublisher publisher) {
        this.orderFacade = orderFacade;
        this.publisher = publisher;
    }

    @EventListener
    public void on(NewOrdersEvent event) {
        if (event == null || event.orders() == null || event.userId() == null) {
            return;
        }

        List<SaveNewOrdersAction.OrderInput> orders = event.orders().stream()
                .filter(o -> o != null)
                .map(o -> new SaveNewOrdersAction.OrderInput(
                        o.externalId(),
                        o.productId(),
                        o.side(),
                        o.status(),
                        o.occurredAt(),
                        o.quantity(),
                        o.price(),
                        o.fee(),
                        o.total()
                ))
                .toList();

        orderFacade.handle(new SaveNewOrdersAction(event.userId(), orders));

        List<Integer> years = event.orders().stream()
                .filter(Objects::nonNull)
                .map(NewOrdersEvent.OrderPayload::occurredAt)
                .filter(Objects::nonNull)
                .map(i -> ZonedDateTime.ofInstant(i, ZONE).getYear())
                .distinct()
                .sorted()
                .toList();

        if (!years.isEmpty()) {
            publisher.publishEvent(new UserOrdersChangedEvent(event.userId(), years));
        }
    }
}


