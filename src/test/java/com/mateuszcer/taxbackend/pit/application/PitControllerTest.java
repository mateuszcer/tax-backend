package com.mateuszcer.taxbackend.pit.application;

import com.mateuszcer.taxbackend.pit.domain.PitFacade;
import com.mateuszcer.taxbackend.pit.domain.PitPreview;
import com.mateuszcer.taxbackend.pit.domain.PitReport;
import com.mateuszcer.taxbackend.pit.domain.action.GeneratePitReportAction;
import com.mateuszcer.taxbackend.pit.domain.query.PitPreviewQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PitControllerTest {

    @Mock
    private PitFacade pitFacade;

    @InjectMocks
    private PitController pitController;

    @Test
    void previewReturns200() {
        when(pitFacade.handle(any(PitPreviewQuery.class))).thenReturn(new PitPreview(2024, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of()));
        ResponseEntity response = pitController.preview("u1", 2024);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void generateReturns200() {
        PitReport r = new PitReport();
        r.setUserId("u1");
        r.setTaxYear(2024);
        r.setCost(BigDecimal.ZERO);
        r.setProceeds(BigDecimal.ZERO);
        r.setGain(BigDecimal.ZERO);
        when(pitFacade.handle(any(GeneratePitReportAction.class))).thenReturn(r);
        ResponseEntity response = pitController.generate("u1", 2024);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}


