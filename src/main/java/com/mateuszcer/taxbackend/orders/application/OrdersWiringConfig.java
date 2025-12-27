package com.mateuszcer.taxbackend.orders.application;

import com.mateuszcer.taxbackend.orders.domain.OrderFacade;
import com.mateuszcer.taxbackend.orders.domain.port.OrderStore;
import com.mateuszcer.taxbackend.orders.domain.usecase.GetUserOrders;
import com.mateuszcer.taxbackend.orders.domain.usecase.SaveNewOrders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrdersWiringConfig {

    @Bean
    public GetUserOrders getUserOrders(OrderStore orderStore) {
        return new GetUserOrders(orderStore);
    }

    @Bean
    public SaveNewOrders saveNewOrders(OrderStore orderStore) {
        return new SaveNewOrders(orderStore);
    }

    @Bean
    public OrderFacade orderFacade(GetUserOrders getUserOrders, SaveNewOrders saveNewOrders) {
        return new OrderFacade(getUserOrders, saveNewOrders);
    }
}


