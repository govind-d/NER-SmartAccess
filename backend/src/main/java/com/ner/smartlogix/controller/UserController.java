package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.ApiResponse;
import com.ner.smartlogix.dto.request.UpdateRolesRequest;
import com.ner.smartlogix.dto.request.UpdateUserRequest;
import com.ner.smartlogix.dto.response.RoleResponse;
import com.ner.smartlogix.dto.response.UserResponse;
import com.ner.smartlogix.enums.RoleName;
import com.ner.smartlogix.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User administration.
 *
 * <p>{@code @PreAuthorize("hasRole('ADMIN')")} on the class applies to every method.
 * Spring evaluates it before the method runs, and a caller without the role never
 * reaches the service - the request ends as a 403 produced by JwtAccessDeniedHandler.
 *
 * <p>Remember that {@code hasRole('ADMIN')} matches the authority {@code ROLE_ADMIN};
 * the prefix is added in {@code UserPrincipal}.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User administration", description = "Manage accounts and roles (ADMIN only)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Paged, filterable list of users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> list(
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.search(role, districtId, q, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One user by id")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user profile")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated",
                userService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable an account")
    public ResponseEntity<ApiResponse<UserResponse>> setStatus(
            @PathVariable Long id, @RequestParam boolean enabled) {
        return ResponseEntity.ok(ApiResponse.success(
                enabled ? "User enabled" : "User disabled",
                userService.setEnabled(id, enabled)));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Replace the roles of a user")
    public ResponseEntity<ApiResponse<UserResponse>> updateRoles(
            @PathVariable Long id, @Valid @RequestBody UpdateRolesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Roles updated",
                userService.updateRoles(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user (prefer disabling instead)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("User deleted"));
    }

    @GetMapping("/roles")
    @Operation(summary = "All roles available in the system")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success(userService.listRoles()));
    }
}
