package com.mateuszcer.taxbackend.pit.application;

import com.mateuszcer.taxbackend.pit.domain.PitFacade;
import com.mateuszcer.taxbackend.pit.domain.PitPreview;
import com.mateuszcer.taxbackend.pit.domain.PitReport;
import com.mateuszcer.taxbackend.pit.domain.action.GeneratePitReportAction;
import com.mateuszcer.taxbackend.pit.domain.query.PitPreviewQuery;
import com.mateuszcer.taxbackend.shared.authuserid.AuthUserId;
import com.mateuszcer.taxbackend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pit/{taxYear}")
@Tag(name = "PIT", description = "PIT calculation")
@SecurityRequirement(name = "bearerAuth")
public class PitController {

    private final PitFacade pitFacade;

    public PitController(PitFacade pitFacade) {
        this.pitFacade = pitFacade;
    }

    @GetMapping("/preview")
    @Operation(summary = "Preview PIT", description = "Calculates totals (cost, proceeds, gain) for selected tax year")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Preview calculated",
                    content = @Content(schema = @Schema(implementation = PitPreviewResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<PitPreviewResponse>> preview(@AuthUserId String authUserId, @PathVariable int taxYear) {
        PitPreview preview = pitFacade.handle(new PitPreviewQuery(authUserId, taxYear));
        return ResponseEntity.ok(ApiResponse.success(PitPreviewResponse.from(preview), "PIT preview calculated"));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate PIT", description = "Generates and stores PIT report for selected tax year")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Report generated",
                    content = @Content(schema = @Schema(implementation = PitReportResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Missing/invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<PitReportResponse>> generate(@AuthUserId String authUserId, @PathVariable int taxYear) {
        PitReport report = pitFacade.handle(new GeneratePitReportAction(authUserId, taxYear));
        return ResponseEntity.ok(ApiResponse.success(PitReportResponse.from(report), "PIT report generated"));
    }

    @Schema(name = "PitPreview")
    public record PitPreviewResponse(
            int taxYear,
            BigDecimal cost,
            BigDecimal proceeds,
            BigDecimal gain,
            List<String> warnings
    ) {
        public static PitPreviewResponse from(PitPreview preview) {
            return new PitPreviewResponse(preview.taxYear(), preview.cost(), preview.proceeds(), preview.gain(), preview.warnings());
        }
    }

    @Schema(name = "PitReport")
    public record PitReportResponse(
            Long id,
            int taxYear,
            BigDecimal cost,
            BigDecimal proceeds,
            BigDecimal gain
    ) {
        public static PitReportResponse from(PitReport report) {
            return new PitReportResponse(report.getId(), report.getTaxYear(), report.getCost(), report.getProceeds(), report.getGain());
        }
    }
}


