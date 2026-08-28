package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.response.WeatherResponse;
import com.ner.smartlogix.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "Weather", description = "District weather feeding the risk engine")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/districts/{code}/current")
    public ResponseEntity<ApiResponse<WeatherResponse>> current(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(weatherService.current(code)));
    }

    @GetMapping("/districts/{code}/history")
    public ResponseEntity<ApiResponse<List<WeatherResponse>>> history(
            @PathVariable String code, @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.success(weatherService.history(code, days)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<List<WeatherResponse>>> summary() {
        return ResponseEntity.ok(ApiResponse.success(weatherService.summary()));
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force a weather pull for every district right now")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> refresh() {
        return ResponseEntity.ok(ApiResponse.success("Weather refreshed",
                Map.of("districtsUpdated", weatherService.refreshAll())));
    }
}
