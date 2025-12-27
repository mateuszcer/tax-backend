package com.mateuszcer.taxbackend.orders.domain.port;

import com.mateuszcer.taxbackend.orders.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderStore {
    Optional<Order> findByUserIdAndExternalId(String userId, String externalId);

    List<Order> findByUserIdOrderByOccurredAtDesc(String userId);

    Order save(Order order);
}


