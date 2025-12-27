package com.mateuszcer.taxbackend.orders.application;

import com.mateuszcer.taxbackend.orders.domain.OrderFacade;
import com.mateuszcer.taxbackend.orders.domain.action.SaveNewOrdersAction;
import com.mateuszcer.taxbackend.shared.events.NewOrdersEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NewOrdersEventConsumer {

    private final OrderFacade orderFacade;

    public NewOrdersEventConsumer(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
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
    }
}


