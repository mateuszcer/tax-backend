package com.mateuszcer.taxbackend.shared.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record NewOrdersEvent(String userId, List<OrderPayload> orders) {
    public record OrderPayload(
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


