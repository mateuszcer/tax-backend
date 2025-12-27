package com.mateuszcer.taxbackend.orders.domain.usecase;

import com.mateuszcer.taxbackend.orders.domain.Order;
import com.mateuszcer.taxbackend.orders.domain.action.SaveNewOrdersAction;
import com.mateuszcer.taxbackend.orders.domain.port.OrderStore;

public class SaveNewOrders {

    private final OrderStore orderStore;

    public SaveNewOrders(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    public void execute(SaveNewOrdersAction action) {
        if (action == null || action.userId() == null || action.orders() == null) {
            return;
        }

        for (SaveNewOrdersAction.OrderInput o : action.orders()) {
            if (o == null || o.externalId() == null || o.productId() == null || o.side() == null || o.status() == null || o.occurredAt() == null) {
                continue;
            }

            if (orderStore.findByUserIdAndExternalId(action.userId(), o.externalId()).isPresent()) {
                continue;
            }

            Order order = new Order();
            order.setUserId(action.userId());
            order.setExternalId(o.externalId());
            order.setProductId(o.productId());
            order.setSide(o.side());
            order.setStatus(o.status());
            order.setOccurredAt(o.occurredAt());
            order.setQuantity(o.quantity());
            order.setPrice(o.price());
            order.setFee(o.fee());
            order.setTotal(o.total());
            orderStore.save(order);
        }
    }
}


