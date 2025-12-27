package com.mateuszcer.taxbackend.orders.domain.usecase;

import com.mateuszcer.taxbackend.orders.domain.Order;
import com.mateuszcer.taxbackend.orders.domain.port.OrderStore;
import com.mateuszcer.taxbackend.orders.domain.query.GetUserOrdersQuery;

import java.util.List;

public class GetUserOrders {

    private final OrderStore orderStore;

    public GetUserOrders(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    public List<Order> execute(GetUserOrdersQuery query) {
        return orderStore.findByUserIdOrderByOccurredAtDesc(query.userId());
    }
}


