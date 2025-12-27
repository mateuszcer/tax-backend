package com.mateuszcer.taxbackend.orders.infrastructure;

import com.mateuszcer.taxbackend.orders.domain.Order;
import com.mateuszcer.taxbackend.orders.domain.port.OrderStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrderJpaStore implements OrderStore {

    private final OrderRepository orderRepository;

    public OrderJpaStore(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Optional<Order> findByUserIdAndExternalId(String userId, String externalId) {
        return orderRepository.findByUserIdAndExternalId(userId, externalId);
    }

    @Override
    public List<Order> findByUserIdOrderByOccurredAtDesc(String userId) {
        return orderRepository.findByUserIdOrderByOccurredAtDesc(userId);
    }

    @Override
    public Order save(Order order) {
        return orderRepository.save(order);
    }
}


