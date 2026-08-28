package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.request.BridgeRequest;
import com.ner.smartlogix.dto.response.BridgeResponse;
import com.ner.smartlogix.enums.BridgeStatus;
import com.ner.smartlogix.service.BridgeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bridges")
@RequiredArgsConstructor
@Tag(name = "Bridges", description = "Bridges and their load restrictions")
public class BridgeController {

    private final BridgeService bridgeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BridgeResponse>>> list(
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) BridgeStatus status) {
        return ResponseEntity.ok(ApiResponse.success(bridgeService.findAll(roadId, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BridgeResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bridgeService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<BridgeResponse>> create(
            @Valid @RequestBody BridgeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bridge created", bridgeService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<BridgeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody BridgeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bridge updated",
                bridgeService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','AUTHORITY_OFFICIAL')")
    public ResponseEntity<ApiResponse<BridgeResponse>> changeStatus(
            @PathVariable Long id, @RequestParam BridgeStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Bridge status updated",
                bridgeService.changeStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bridgeService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Bridge deleted"));
    }
}
