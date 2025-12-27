package com.mateuszcer.taxbackend.pit.infrastructure;

import com.mateuszcer.taxbackend.config.TestSecurityConfig;
import com.mateuszcer.taxbackend.pit.domain.PitReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PitReportRepositoryTest {

    @Autowired
    private PitReportRepository repository;

    @Test
    void saveAndFindByUserAndYear() {
        PitReport r = new PitReport();
        r.setUserId("u1");
        r.setTaxYear(2024);
        r.setCost(new BigDecimal("10.00"));
        r.setProceeds(new BigDecimal("20.00"));
        r.setGain(new BigDecimal("10.00"));
        repository.save(r);

        var found = repository.findByUserIdAndTaxYear("u1", 2024);
        assertThat(found).isPresent();
        assertThat(found.get().getGain()).isEqualByComparingTo("10.00");
    }
}


