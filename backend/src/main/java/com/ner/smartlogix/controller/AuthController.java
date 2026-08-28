package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.request.*;
import com.ner.smartlogix.dto.response.AuthResponse;
import com.ner.smartlogix.dto.response.UserResponse;
import com.ner.smartlogix.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 *
 * <p>Notice how thin this class is. A controller should only: accept the request,
 * trigger validation with {@code @Valid}, call one service method, and wrap the result.
 * No business rule, no repository call and no try/catch belongs here - exceptions are
 * handled centrally by {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, registration, tokens and passwords")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Log in and receive an access token plus a refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful",
                authService.login(request)));
    }

    @PostMapping("/register")
    @Operation(summary = "Create a user (ADMIN only, except the first user in an empty database)")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        // 201 Created is the correct status for "a new resource now exists".
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", created));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed",
                authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke every refresh token of the current user")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Profile and roles of the caller")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success(authService.currentUser()));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change your own password; ends all your other sessions")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.message("Password changed successfully"));
    }
}
