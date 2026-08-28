package com.ner.smartlogix.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Administrator edit of a user profile. Password and roles are changed elsewhere. */
public record UpdateUserRequest(
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 120) String fullName,
        @Pattern(regexp = "^$|^[0-9]{10}$", message = "Phone must be 10 digits") String phone,
        String districtCode) {
}
