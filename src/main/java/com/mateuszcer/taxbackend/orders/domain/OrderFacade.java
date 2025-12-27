package com.mateuszcer.taxbackend.orders.domain;

import com.mateuszcer.taxbackend.orders.domain.action.SaveNewOrdersAction;
import com.mateuszcer.taxbackend.orders.domain.query.GetUserOrdersQuery;
import com.mateuszcer.taxbackend.orders.domain.usecase.GetUserOrders;
import com.mateuszcer.taxbackend.orders.domain.usecase.SaveNewOrders;

import java.util.List;

public class OrderFacade {

    private final GetUserOrders getUserOrders;
    private final SaveNewOrders saveNewOrders;

    public OrderFacade(GetUserOrders getUserOrders, SaveNewOrders saveNewOrders) {
        this.getUserOrders = getUserOrders;
        this.saveNewOrders = saveNewOrders;
    }

    public List<Order> handle(GetUserOrdersQuery query) {
        return getUserOrders.execute(query);
    }

    public void handle(SaveNewOrdersAction action) {
        saveNewOrders.execute(action);
    }
}


