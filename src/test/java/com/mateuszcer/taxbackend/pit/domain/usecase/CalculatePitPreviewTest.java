package com.mateuszcer.taxbackend.pit.domain.usecase;

import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsPreview;
import com.mateuszcer.taxbackend.capitalgains.domain.CapitalGainsFacade;
import com.mateuszcer.taxbackend.capitalgains.domain.query.CapitalGainsPreviewQuery;
import com.mateuszcer.taxbackend.pit.domain.PitPreview;
import com.mateuszcer.taxbackend.pit.domain.query.PitPreviewQuery;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatePitPreviewTest {

    @Test
    void mapsCapitalGainsPreviewToPitPreview() {
        CapitalGainsFacade capitalGains = new CapitalGainsFacade(null, null) {
            @Override
            public CapitalGainsPreview handle(CapitalGainsPreviewQuery query) {
                return new CapitalGainsPreview(
                        query.taxYear(),
                        new BigDecimal("26.00"),
                        new BigDecimal("37.50"),
                        new BigDecimal("11.50"),
                        java.util.List.of()
                );
            }
        };

        CalculatePitPreview usecase = new CalculatePitPreview(capitalGains);
        PitPreview preview = usecase.execute(new PitPreviewQuery("u1", 2024));

        assertThat(preview.cost()).isEqualByComparingTo("26.00");
        assertThat(preview.proceeds()).isEqualByComparingTo("37.50");
        assertThat(preview.gain()).isEqualByComparingTo("11.50");
    }
}


