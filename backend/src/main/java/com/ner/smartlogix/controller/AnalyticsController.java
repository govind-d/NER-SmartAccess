package com.ner.smartlogix.controller;

import com.ner.smartlogix.analytics.DashboardService;
import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.response.ChartPointResponse;
import com.ner.smartlogix.dto.response.DashboardSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Everything the analytics dashboard needs. */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL','LOGISTICS_MANAGER')")
@Tag(name = "Analytics", description = "Dashboard summary cards and charts")
public class AnalyticsController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "All summary cards in one call")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.summary()));
    }

    @GetMapping("/deliveries/trend")
    public ResponseEntity<ApiResponse<List<ChartPointResponse>>> deliveryTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.deliveryTrend(days)));
    }

    @GetMapping("/incidents/by-type")
    public ResponseEntity<ApiResponse<List<ChartPointResponse>>> incidentsByType(
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.incidentsByType(days)));
    }

    @GetMapping("/roads/status-distribution")
    public ResponseEntity<ApiResponse<List<ChartPointResponse>>> roadStatus() {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.roadStatusDistribution()));
    }

    @GetMapping("/districts/accessibility")
    public ResponseEntity<ApiResponse<List<ChartPointResponse>>> districtAccessibility() {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.districtAccessibility()));
    }

    @GetMapping("/disruption-statistics")
    public ResponseEntity<ApiResponse<List<ChartPointResponse>>> disruptions(
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.disruptionStatistics(days)));
    }

    @GetMapping("/delay-analysis")
    public ResponseEntity<ApiResponse<List<ChartPointResponse>>> delayAnalysis() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.delayAnalysis()));
    }
}
