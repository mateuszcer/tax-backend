package com.mateuszcer.taxbackend.capitalgains.domain.usecase;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsPreview;
import com.mateuszcer.taxbackend.capitalgains.domain.OrderSnapshot;
import com.mateuszcer.taxbackend.capitalgains.domain.port.UserOrdersProvider;
import com.mateuszcer.taxbackend.capitalgains.domain.query.CapitalGainsPreviewQuery;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculateCapitalGainsPreviewTest {

    @Test
    void fifoCalculatesCostAndProceeds() {
        UserOrdersProvider provider = userId -> List.of(
                new OrderSnapshot("AAA-PLN", "BUY", "FILLED", Instant.parse("2024-01-01T10:00:00Z"),
                        new BigDecimal("2"), new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("20")),
                new OrderSnapshot("AAA-PLN", "BUY", "FILLED", Instant.parse("2024-01-02T10:00:00Z"),
                        new BigDecimal("1"), new BigDecimal("12"), BigDecimal.ZERO, new BigDecimal("12")),
                new OrderSnapshot("AAA-PLN", "SELL", "FILLED", Instant.parse("2024-02-01T10:00:00Z"),
                        new BigDecimal("2.5"), new BigDecimal("15"), BigDecimal.ZERO, new BigDecimal("37.5"))
        );

        CalculateCapitalGainsPreview usecase = new CalculateCapitalGainsPreview(provider);
        CapitalGainsPreview preview = usecase.execute(new CapitalGainsPreviewQuery("u1", 2024));

        assertThat(preview.cost()).isEqualByComparingTo("26.00");
        assertThat(preview.proceeds()).isEqualByComparingTo("37.50");
        assertThat(preview.gain()).isEqualByComparingTo("11.50");
    }
}


