package com.ner.smartlogix.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Exchanges a still-valid refresh token for a new short-lived access token. */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken) {
}
