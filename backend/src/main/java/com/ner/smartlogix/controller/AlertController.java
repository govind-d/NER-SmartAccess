package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.request.AlertRequest;
import com.ner.smartlogix.dto.response.AlertResponse;
import com.ner.smartlogix.dto.response.NotificationResponse;
import com.ner.smartlogix.enums.Severity;
import com.ner.smartlogix.service.AlertService;
import com.ner.smartlogix.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Real-time alerts and personal notifications")
public class AlertController {

    private final AlertService alertService;
    private final NotificationService notificationService;

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<Page<AlertResponse>>> search(
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) Long districtId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                alertService.search(active, severity, districtId, pageable)));
    }

    @GetMapping("/alerts/recent")
    @Operation(summary = "Newest active alerts, for the dashboard panel")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> recent(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(alertService.recent(limit)));
    }

    @PostMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    @Operation(summary = "Broadcast a manual alert to everyone")
    public ResponseEntity<ApiResponse<AlertResponse>> create(
            @Valid @RequestBody AlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Alert broadcast",
                        alertService.createManual(request)));
    }

    @PatchMapping("/alerts/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        alertService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.message("Alert deactivated"));
    }

    // ---------------------------------------------------------------- notifications

    @GetMapping("/notifications/my")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> mine(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.findMine(unreadOnly, pageable)));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("unread", notificationService.unreadCount())));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(ApiResponse.message("Notification marked as read"));
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(ApiResponse.message("All notifications marked as read"));
    }
}
