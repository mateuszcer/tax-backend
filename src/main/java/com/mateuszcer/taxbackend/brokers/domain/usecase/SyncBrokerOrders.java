package com.mateuszcer.taxbackend.brokers.domain.usecase;

import com.mateuszcer.taxbackend.brokers.domain.ActionResult;
import com.mateuszcer.taxbackend.brokers.domain.action.SyncBrokerOrdersAction;
import com.mateuszcer.taxbackend.brokers.domain.port.BrokerAdapter;
import com.mateuszcer.taxbackend.brokers.domain.port.NewOrdersPublisher;
import com.mateuszcer.taxbackend.shared.events.NewOrdersEvent;

import java.util.List;

public class SyncBrokerOrders {

    private final NewOrdersPublisher newOrdersPublisher;

    public SyncBrokerOrders(NewOrdersPublisher newOrdersPublisher) {
        this.newOrdersPublisher = newOrdersPublisher;
    }

    public ActionResult<Integer> execute(SyncBrokerOrdersAction action, BrokerAdapter adapter) {
        if (action == null || adapter == null || action.userId() == null) {
            return ActionResult.failure("Invalid sync request.");
        }

        ActionResult<List<NewOrdersEvent.OrderPayload>> result = adapter.getOrdersPayload(action.userId());
        if (!result.isSuccess()) {
            return ActionResult.failure(result.getMessage());
        }

        List<NewOrdersEvent.OrderPayload> orders = result.getData();
        if (orders == null || orders.isEmpty()) {
            return ActionResult.success(0);
        }

        newOrdersPublisher.publish(new NewOrdersEvent(action.userId(), orders));
        return ActionResult.success(orders.size());
    }
}


