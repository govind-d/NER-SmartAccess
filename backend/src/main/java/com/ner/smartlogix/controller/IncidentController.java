package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.geo.GeoJsonFeatureCollection;
import com.ner.smartlogix.dto.request.IncidentRequest;
import com.ner.smartlogix.dto.response.IncidentResponse;
import com.ner.smartlogix.enums.IncidentStatus;
import com.ner.smartlogix.enums.IncidentType;
import com.ner.smartlogix.enums.Severity;
import com.ner.smartlogix.service.FileStorageService;
import com.ner.smartlogix.service.GeoJsonService;
import com.ner.smartlogix.service.IncidentService;
import com.ner.smartlogix.service.impl.IncidentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Incident reporting.
 *
 * <p>The create endpoint accepts {@code multipart/form-data} so a photo can travel with
 * the report in one request - which matters on a weak connection, where two round trips
 * are twice as likely to fail.
 */
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Field reports of disruptions")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentServiceImpl incidentServiceImpl;
    private final GeoJsonService geoJsonService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<IncidentResponse>>> search(
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Long districtId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                incidentService.search(type, severity, status, districtId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(incidentService.findById(id)));
    }

    @GetMapping("/geojson")
    @Operation(summary = "Incident pins for the map")
    public ResponseEntity<GeoJsonFeatureCollection> geoJson(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(geoJsonService.incidents(activeOnly));
    }

    @GetMapping("/near")
    public ResponseEntity<ApiResponse<List<IncidentResponse>>> near(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5000") double radiusMeters) {
        return ResponseEntity.ok(ApiResponse.success(
                incidentService.findNear(lat, lon, radiusMeters)));
    }

    /** Multipart: the JSON body arrives as the "data" part, the image as "photo". */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Report an incident with a photo. Idempotent on clientUuid.")
    public ResponseEntity<ApiResponse<IncidentResponse>> reportWithPhoto(
            @Valid @RequestPart("data") IncidentRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Incident reported",
                        incidentService.report(request, photo)));
    }

    /** Plain JSON variant, used by the offline queue where the photo is synced separately. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Report an incident without a photo")
    public ResponseEntity<ApiResponse<IncidentResponse>> report(
            @Valid @RequestBody IncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Incident reported",
                        incidentService.report(request, null)));
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable Long id) {
        Resource file = fileStorageService.load(incidentServiceImpl.photoFileName(id));
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(file);
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<IncidentResponse>> verify(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Incident verified",
                incidentService.verify(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<IncidentResponse>> changeStatus(
            @PathVariable Long id, @RequestParam IncidentStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Incident status updated",
                incidentService.changeStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        incidentService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Incident deleted"));
    }
}
