package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

/** Replaces the complete role set of a user. */
public record UpdateRolesRequest(
        @NotEmpty(message = "At least one role is required")
        Set<RoleName> roles) {
}
