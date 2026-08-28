package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.request.GpsPingRequest;
import com.ner.smartlogix.dto.request.VehicleRequest;
import com.ner.smartlogix.dto.response.VehicleLocationResponse;
import com.ner.smartlogix.dto.response.VehicleResponse;
import com.ner.smartlogix.enums.VehicleType;
import com.ner.smartlogix.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Fleet management and the GPS stream. */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Fleet and live GPS tracking")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) VehicleType type) {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.findAll(active, type)));
    }

    @GetMapping("/live")
    @Operation(summary = "Last known position of every active vehicle, for the map")
    public ResponseEntity<ApiResponse<List<VehicleLocationResponse>>> live() {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.liveFleet()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<VehicleResponse>> create(
            @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle created", vehicleService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<VehicleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Vehicle updated",
                vehicleService.update(id, request)));
    }

    @PatchMapping("/{id}/driver")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<VehicleResponse>> assignDriver(
            @PathVariable Long id, @RequestParam(required = false) Long driverId) {
        return ResponseEntity.ok(ApiResponse.success("Driver assigned",
                vehicleService.assignDriver(id, driverId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Vehicle deleted"));
    }

    // ---------------------------------------------------------------- GPS

    @PostMapping("/{id}/locations")
    @Operation(summary = "Report a GPS position (drivers may report only their own vehicle)")
    public ResponseEntity<ApiResponse<VehicleLocationResponse>> reportPosition(
            @PathVariable Long id, @Valid @RequestBody GpsPingRequest ping) {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.recordPosition(id, ping)));
    }

    @GetMapping("/{id}/locations/latest")
    public ResponseEntity<ApiResponse<VehicleLocationResponse>> latest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.latestPosition(id)));
    }

    @GetMapping("/{id}/locations/history")
    @Operation(summary = "Position trail, default the last six hours")
    public ResponseEntity<ApiResponse<List<VehicleLocationResponse>>> history(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(vehicleService.history(id, from, to)));
    }
}
