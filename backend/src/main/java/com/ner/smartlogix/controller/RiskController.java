package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.request.RiskPredictionRequest;
import com.ner.smartlogix.dto.response.RiskAssessmentResponse;
import com.ner.smartlogix.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * The AI module's public face.
 *
 * <p>{@code POST /risk/predict} is the endpoint to demonstrate: send the same road with
 * 20 mm of rain and then with 200 mm, and watch LOW_RISK become HIGH_RISK with a score
 * card explaining exactly which rules fired and by how much.
 */
@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
@Tag(name = "Risk prediction", description = "Java rule-based disruption risk engine")
public class RiskController {

    private final RiskService riskService;

    @PostMapping("/predict")
    @Operation(summary = "What-if prediction; nothing is stored")
    public ResponseEntity<ApiResponse<RiskAssessmentResponse>> predict(
            @Valid @RequestBody RiskPredictionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(riskService.predictWhatIf(request)));
    }

    @GetMapping("/roads/{roadId}")
    @Operation(summary = "Current risk and score card for one road")
    public ResponseEntity<ApiResponse<RiskAssessmentResponse>> currentRisk(
            @PathVariable Long roadId) {
        return ResponseEntity.ok(ApiResponse.success(riskService.currentRisk(roadId)));
    }

    @PostMapping("/roads/{roadId}/assess")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    @Operation(summary = "Run the engine now and store the result")
    public ResponseEntity<ApiResponse<RiskAssessmentResponse>> assess(
            @PathVariable Long roadId) {
        return ResponseEntity.ok(ApiResponse.success("Assessment stored",
                riskService.assessRoad(roadId)));
    }

    @GetMapping("/roads/{roadId}/history")
    public ResponseEntity<ApiResponse<List<RiskAssessmentResponse>>> history(
            @PathVariable Long roadId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success(riskService.history(roadId, days)));
    }

    @PostMapping("/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    @Operation(summary = "Re-score the whole network, or one district")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recalculate(
            @RequestParam(required = false) Long districtId) {
        int changed = riskService.reassessAll(districtId);
        return ResponseEntity.ok(ApiResponse.success("Re-assessment complete",
                Map.of("roadsChanged", changed)));
    }

    @GetMapping("/model/info")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Which prediction model is currently wired in")
    public ResponseEntity<ApiResponse<Map<String, Object>>> modelInfo() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "activeModel", riskService.activeModelName(),
                "type", "rule-based weighted scoring",
                "swappableWith", "Tribuo RandomForest (Phase 8)")));
    }
}
