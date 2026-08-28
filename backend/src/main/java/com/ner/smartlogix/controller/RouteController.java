package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.request.RouteRecommendationRequest;
import com.ner.smartlogix.dto.response.RouteRecommendationResponse;
import com.ner.smartlogix.dto.response.RouteResponse;
import com.ner.smartlogix.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Smart route recommendation: the heart of the optimisation module. */
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Routes", description = "Risk-aware route recommendation")
public class RouteController {

    private final RouteService routeService;

    @PostMapping("/recommend")
    @Operation(summary = "Best route plus alternates, scored against live conditions")
    public ResponseEntity<ApiResponse<RouteRecommendationResponse>> recommend(
            @Valid @RequestBody RouteRecommendationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Route computed",
                routeService.recommend(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(routeService.findById(id)));
    }

    @PostMapping("/{id}/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER')")
    @Operation(summary = "Re-score a stored route against current weather and incidents")
    public ResponseEntity<ApiResponse<RouteResponse>> recalculate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Route re-scored",
                routeService.recalculate(id)));
    }
}
