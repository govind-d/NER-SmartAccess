package com.ner.smartlogix.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials.
 *
 * <p>The validation annotations are enforced by {@code @Valid} in the controller, so an
 * empty username is rejected with a 400 before any database call happens.
 */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password) {
}
