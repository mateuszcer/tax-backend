package com.mateuszcer.taxbackend.pit.domain;

import java.math.BigDecimal;
import java.util.List;

public record PitPreview(
        int taxYear,
        BigDecimal cost,
        BigDecimal proceeds,
        BigDecimal gain,
        List<String> warnings
) {
}


