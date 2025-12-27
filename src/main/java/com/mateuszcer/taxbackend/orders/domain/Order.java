package com.mateuszcer.taxbackend.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_user_id_occurred_at", columnList = "user_id, occurred_at")
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "side", nullable = false)
    private String side;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "quantity", precision = 38, scale = 18)
    private BigDecimal quantity;

    @Column(name = "price", precision = 38, scale = 18)
    private BigDecimal price;

    @Column(name = "fee", precision = 38, scale = 18)
    private BigDecimal fee;

    @Column(name = "total", precision = 38, scale = 18)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}


