package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.geo.GeoJsonFeatureCollection;
import com.ner.smartlogix.dto.request.DistrictRequest;
import com.ner.smartlogix.dto.response.AccessibilityResponse;
import com.ner.smartlogix.dto.response.DistrictResponse;
import com.ner.smartlogix.service.DistrictService;
import com.ner.smartlogix.service.GeoJsonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Districts, their accessibility, and the boundary layer for the map. */
@RestController
@RequestMapping("/api/v1/districts")
@RequiredArgsConstructor
@Tag(name = "Districts", description = "District registry and accessibility")
public class DistrictController {

    private final DistrictService districtService;
    private final GeoJsonService geoJsonService;

    @GetMapping
    @Operation(summary = "All districts with their current accessibility")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> list(
            @RequestParam(required = false) String state) {
        return ResponseEntity.ok(ApiResponse.success(districtService.findAll(state)));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<DistrictResponse>> byCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(districtService.findByCode(code)));
    }

    @GetMapping("/geojson")
    @Operation(summary = "District boundaries as GeoJSON, ready for Leaflet")
    public ResponseEntity<GeoJsonFeatureCollection> geoJson() {
        // Returned bare rather than wrapped: L.geoJSON() expects a FeatureCollection.
        return ResponseEntity.ok(geoJsonService.districts());
    }

    @GetMapping("/{code}/accessibility")
    public ResponseEntity<ApiResponse<AccessibilityResponse>> accessibility(
            @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(
                districtService.currentAccessibility(code)));
    }

    @GetMapping("/accessibility/summary")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL','LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<List<AccessibilityResponse>>> summary() {
        return ResponseEntity.ok(ApiResponse.success(districtService.accessibilitySummary()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<DistrictResponse>> create(
            @Valid @RequestBody DistrictRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("District created", districtService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<DistrictResponse>> update(
            @PathVariable Long id, @Valid @RequestBody DistrictRequest request) {
        return ResponseEntity.ok(ApiResponse.success("District updated",
                districtService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        districtService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("District deleted"));
    }
}
