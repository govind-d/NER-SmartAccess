package com.ner.smartlogix.mapper;

import com.ner.smartlogix.dto.response.RoleResponse;
import com.ner.smartlogix.dto.response.UserResponse;
import com.ner.smartlogix.entity.Role;
import com.ner.smartlogix.entity.User;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts entities into response DTOs.
 *
 * <p>This is an INTERFACE with no implementation, yet it works: MapStruct reads it at
 * compile time and generates {@code UserMapperImpl} into {@code target/generated-sources}.
 * The generated code is plain Java (getters and setters), so there is no reflection at
 * runtime, and a mistake such as a renamed field becomes a compile error rather than a
 * silent null in production.
 *
 * <p>{@code componentModel = "spring"} makes the generated class a {@code @Component},
 * so it can simply be injected into services.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Nested source paths such as {@code district.code} are null-safe: MapStruct
     * generates the null check, so a user without a district maps to null, not a crash.
     */
    @Mapping(target = "districtCode", source = "district.code")
    @Mapping(target = "districtName", source = "district.name")
    UserResponse toResponse(User user);

    RoleResponse toResponse(Role role);

    /**
     * MapStruct finds this method automatically when it needs to turn a
     * {@code Set<Role>} into a {@code Set<String>} for the "roles" field.
     */
    default Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }
}
