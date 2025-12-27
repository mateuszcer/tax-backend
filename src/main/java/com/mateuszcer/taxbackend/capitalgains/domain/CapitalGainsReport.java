package com.mateuszcer.taxbackend.capitalgains.domain;

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
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "capital_gains_report",
        indexes = {
                @Index(name = "idx_capital_gains_report_user_year", columnList = "user_id, tax_year")
        }
)
public class CapitalGainsReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "cost", precision = 38, scale = 18, nullable = false)
    private BigDecimal cost;

    @Column(name = "proceeds", precision = 38, scale = 18, nullable = false)
    private BigDecimal proceeds;

    @Column(name = "gain", precision = 38, scale = 18, nullable = false)
    private BigDecimal gain;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}


