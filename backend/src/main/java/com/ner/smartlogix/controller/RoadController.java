package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.geo.GeoJsonFeatureCollection;
import com.ner.smartlogix.dto.request.RoadRequest;
import com.ner.smartlogix.dto.request.RoadStatusRequest;
import com.ner.smartlogix.dto.response.RoadResponse;
import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.service.GeoJsonService;
import com.ner.smartlogix.service.RoadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** The monitored road network. */
@RestController
@RequestMapping("/api/v1/roads")
@RequiredArgsConstructor
@Tag(name = "Roads", description = "Road network, status and map layer")
public class RoadController {

    private final RoadService roadService;
    private final GeoJsonService geoJsonService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoadResponse>>> search(
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) RoadStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                roadService.search(districtId, status, riskLevel, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoadResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roadService.findById(id)));
    }

    @GetMapping("/geojson")
    @Operation(summary = "Roads as GeoJSON, optionally filtered by status")
    public ResponseEntity<GeoJsonFeatureCollection> geoJson(
            @RequestParam(required = false) RoadStatus status) {
        return ResponseEntity.ok(geoJsonService.roads(status == null ? null : status.name()));
    }

    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse<List<RoadResponse>>> blocked() {
        return ResponseEntity.ok(ApiResponse.success(
                roadService.findByStatus(RoadStatus.BLOCKED)));
    }

    @GetMapping("/high-risk")
    public ResponseEntity<ApiResponse<List<RoadResponse>>> highRisk() {
        return ResponseEntity.ok(ApiResponse.success(
                roadService.findByStatus(RoadStatus.HIGH_RISK)));
    }

    @GetMapping("/near")
    @Operation(summary = "Roads within a radius of a point (PostGIS ST_DWithin)")
    public ResponseEntity<ApiResponse<List<RoadResponse>>> near(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "1000") double radiusMeters) {
        return ResponseEntity.ok(ApiResponse.success(
                roadService.findNear(lat, lon, radiusMeters)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<RoadResponse>> create(
            @Valid @RequestBody RoadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Road created", roadService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<RoadResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RoadRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Road updated",
                roadService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    @Operation(summary = "Open or close a road; raises alerts and re-evaluates the district")
    public ResponseEntity<ApiResponse<RoadResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody RoadStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Road status updated",
                roadService.changeStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roadService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Road deleted"));
    }
}
