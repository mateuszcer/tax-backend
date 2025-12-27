package com.mateuszcer.taxbackend.capitalgains.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSnapshot(
        String productId,
        String side,
        String status,
        Instant occurredAt,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        BigDecimal total
) {
}


