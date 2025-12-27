package com.mateuszcer.taxbackend.orders.domain.action;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaveNewOrdersAction(String userId, List<OrderInput> orders) {
    public record OrderInput(
            String externalId,
            String productId,
            String side,
            String status,
            Instant occurredAt,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal fee,
            BigDecimal total
    ) {}
}


