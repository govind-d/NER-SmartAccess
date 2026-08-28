package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.request.FieldReportSyncRequest;
import com.ner.smartlogix.dto.response.FieldReportResponse;
import com.ner.smartlogix.dto.response.SyncResultResponse;
import com.ner.smartlogix.service.FieldReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** The endpoint the offline progressive web app talks to when connectivity returns. */
@RestController
@RequestMapping("/api/v1/field-reports")
@RequiredArgsConstructor
@Tag(name = "Field reports", description = "Offline-first reporting and synchronisation")
public class FieldReportController {

    private final FieldReportService fieldReportService;

    @PostMapping("/sync")
    @Operation(summary = "Upload a batch of reports queued while offline; per-item results")
    public ResponseEntity<ApiResponse<SyncResultResponse>> sync(
            @Valid @RequestBody FieldReportSyncRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sync processed",
                fieldReportService.sync(request)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<FieldReportResponse>>> mine(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(fieldReportService.findMine(pageable)));
    }

    @GetMapping("/pending-review")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    @Operation(summary = "Reports not yet promoted into a verified incident")
    public ResponseEntity<ApiResponse<Page<FieldReportResponse>>> pending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                fieldReportService.findPendingReview(pageable)));
    }

    @PostMapping("/{id}/promote")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    @Operation(summary = "Turn a reviewed report into a real incident")
    public ResponseEntity<ApiResponse<FieldReportResponse>> promote(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Report promoted to incident",
                fieldReportService.promoteToIncident(id)));
    }
}
