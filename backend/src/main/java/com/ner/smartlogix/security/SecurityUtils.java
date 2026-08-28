package com.ner.smartlogix.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Small helper for "who is calling right now?".
 *
 * <p>The SecurityContext is filled in by {@link JwtAuthenticationFilter} and is stored
 * per request thread, so any service can read it without the controller having to pass
 * the user down through every method signature.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // utility class - never instantiated
    }

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static Optional<Long> currentUserId() {
        return currentPrincipal().map(UserPrincipal::getId);
    }

    /** True if the caller holds the given role (pass the bare name, e.g. "ADMIN"). */
    public static boolean hasRole(String roleName) {
        return currentPrincipal()
                .map(principal -> principal.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName)))
                .orElse(false);
    }
}
