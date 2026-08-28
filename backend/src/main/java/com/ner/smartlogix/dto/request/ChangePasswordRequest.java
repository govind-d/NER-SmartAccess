package com.ner.smartlogix.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The current password is required as well as the new one: possession of a valid access
 * token must not be enough to take over an account from an unattended browser.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        String newPassword) {
}
