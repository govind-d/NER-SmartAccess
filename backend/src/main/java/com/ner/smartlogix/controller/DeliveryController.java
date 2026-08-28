package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.request.DeliveryRequest;
import com.ner.smartlogix.dto.request.DeliveryStatusRequest;
import com.ner.smartlogix.dto.response.DeliveryResponse;
import com.ner.smartlogix.enums.DeliveryStatus;
import com.ner.smartlogix.service.DeliveryService;
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

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
@Tag(name = "Deliveries", description = "Consignments and their lifecycle")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<Page<DeliveryResponse>>> search(
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) Long vehicleId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                deliveryService.search(status, vehicleId, pageable)));
    }

    @GetMapping("/my")
    @Operation(summary = "The calling driver's own consignments")
    public ResponseEntity<ApiResponse<Page<DeliveryResponse>>> mine(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.findMine(pageable)));
    }

    @GetMapping("/delayed")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> delayed() {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.findDelayed()));
    }

    @GetMapping("/track/{trackingCode}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> track(
            @PathVariable String trackingCode) {
        return ResponseEntity.ok(ApiResponse.success(
                deliveryService.findByTrackingCode(trackingCode)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> create(
            @Valid @RequestBody DeliveryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Delivery created", deliveryService.create(request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Advance the lifecycle; an illegal transition returns 422")
    public ResponseEntity<ApiResponse<DeliveryResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody DeliveryStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Delivery status updated",
                deliveryService.changeStatus(id, request.status())));
    }

    @PatchMapping("/{id}/route")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER')")
    @Operation(summary = "Attach a route; recalculates the estimated arrival time")
    public ResponseEntity<ApiResponse<DeliveryResponse>> assignRoute(
            @PathVariable Long id, @RequestParam Long routeId) {
        return ResponseEntity.ok(ApiResponse.success("Route assigned",
                deliveryService.assignRoute(id, routeId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        deliveryService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Delivery deleted"));
    }
}
