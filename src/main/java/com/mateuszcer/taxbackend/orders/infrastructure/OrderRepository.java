package com.mateuszcer.taxbackend.orders.infrastructure;

import com.mateuszcer.taxbackend.orders.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserIdAndExternalId(String userId, String externalId);

    List<Order> findByUserIdOrderByOccurredAtDesc(String userId);
}


