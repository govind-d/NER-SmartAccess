package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.RoleName;
import jakarta.validation.constraints.*;
import java.util.Set;

/**
 * New user details. Only an ADMIN may send this - except for the very first user in an
 * empty database, who bootstraps the system and automatically becomes the ADMIN.
 */
public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                 message = "Username may contain only letters, digits, dot, underscore and hyphen")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 120)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 120)
        String fullName,

        @Pattern(regexp = "^$|^[0-9]{10}$", message = "Phone must be 10 digits")
        String phone,

        String districtCode,

        @NotEmpty(message = "At least one role is required")
        Set<RoleName> roles) {
}
