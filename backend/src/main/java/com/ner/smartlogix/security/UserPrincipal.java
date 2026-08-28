package com.ner.smartlogix.security;

import com.ner.smartlogix.entity.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The adapter between our {@code User} entity and Spring Security.
 *
 * <p>Spring Security only understands {@link UserDetails}; it knows nothing about JPA.
 * Rather than making the entity implement a framework interface (which would drag
 * security concerns into the persistence layer), we wrap it here.
 *
 * <p>Note the {@code "ROLE_"} prefix added below. Spring's {@code hasRole('ADMIN')}
 * silently looks for an authority literally named {@code ROLE_ADMIN}; forgetting this
 * prefix is the single most common reason role checks "mysteriously" fail.
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal from(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.isEnabled(),
                authorities);
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
