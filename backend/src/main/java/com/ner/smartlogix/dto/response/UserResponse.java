package com.ner.smartlogix.dto.response;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * What the API is allowed to say about a user.
 *
 * <p>Compare it with the {@code User} entity: there is no {@code passwordHash} here.
 * That is the entire reason DTOs exist - if controllers returned entities, one careless
 * endpoint would leak every password hash in the database.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String districtCode,
        String districtName,
        boolean enabled,
        Set<String> roles,
        OffsetDateTime createdAt) {
}
