package com.mateuszcer.taxbackend.capitalgains.domain;

import java.math.BigDecimal;
import java.util.List;

public record CapitalGainsPreview(
        int taxYear,
        BigDecimal cost,
        BigDecimal proceeds,
        BigDecimal gain,
        List<String> warnings
) {
}


