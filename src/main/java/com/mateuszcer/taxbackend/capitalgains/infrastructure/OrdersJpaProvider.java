package com.mateuszcer.taxbackend.capitalgains.infrastructure;

import com.mateuszcer.taxbackend.capitalgains.domain.OrderSnapshot;
import com.mateuszcer.taxbackend.capitalgains.domain.port.UserOrdersProvider;
import com.mateuszcer.taxbackend.orders.infrastructure.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrdersJpaProvider implements UserOrdersProvider {

    private final OrderRepository orderRepository;

    public OrdersJpaProvider(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<OrderSnapshot> getForUser(String userId) {
        return orderRepository.findByUserIdOrderByOccurredAtDesc(userId).stream()
                .map(o -> new OrderSnapshot(
                        o.getProductId(),
                        o.getSide(),
                        o.getStatus(),
                        o.getOccurredAt(),
                        o.getQuantity(),
                        o.getPrice(),
                        o.getFee(),
                        o.getTotal()
                ))
                .toList();
    }
}


