package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.AlertType;
import com.ner.smartlogix.enums.Severity;
import jakarta.validation.constraints.*;

/** A manual broadcast by an official, e.g. a curfew or a convoy movement. */
public record AlertRequest(
        @NotNull AlertType alertType,
        @NotNull Severity severity,
        @NotBlank @Size(max = 150) String title,
        @NotBlank String message,
        String districtCode,
        String roadCode,
        @Positive Integer expiresInHours) {
}
