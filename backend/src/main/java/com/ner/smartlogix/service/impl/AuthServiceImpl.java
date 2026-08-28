package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.*;
import com.ner.smartlogix.dto.response.AuthResponse;
import com.ner.smartlogix.dto.response.UserResponse;
import com.ner.smartlogix.entity.RefreshToken;
import com.ner.smartlogix.entity.Role;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.RoleName;
import com.ner.smartlogix.exception.*;
import com.ner.smartlogix.mapper.UserMapper;
import com.ner.smartlogix.repository.*;
import com.ner.smartlogix.security.JwtTokenProvider;
import com.ner.smartlogix.security.SecurityUtils;
import com.ner.smartlogix.security.UserPrincipal;
import com.ner.smartlogix.service.AuthService;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login, registration, token refresh, logout and password change.
 *
 * <p>{@code @Transactional} lives here rather than on the controller because a business
 * operation is the correct transaction boundary: either the whole registration succeeds
 * or none of it does.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DistrictRepository districtRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Spring compares the BCrypt hash for us and throws BadCredentialsException or
        // DisabledException, both of which GlobalExceptionHandler turns into a 401.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshToken = issueRefreshToken(user);

        log.info("User '{}' logged in", user.getUsername());
        return AuthResponse.of(accessToken, refreshToken,
                tokenProvider.getAccessExpirationSeconds(), userMapper.toResponse(user));
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Bootstrap rule: an empty database has no administrator who could create one,
        // so the very first registration is allowed and is promoted to ADMIN.
        boolean bootstrap = userRepository.count() == 0;
        if (!bootstrap && !SecurityUtils.hasRole("ADMIN")) {
            throw new AccessDeniedException("Only an administrator may register new users");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("User", "username", request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        // The plain password exists only inside this method; only the hash is stored.
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);

        if (request.districtCode() != null && !request.districtCode().isBlank()) {
            user.setDistrict(districtRepository.findByCode(request.districtCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "District", "code", request.districtCode())));
        }

        Set<RoleName> requested = new HashSet<>(request.roles());
        if (bootstrap) {
            requested.add(RoleName.ADMIN);
        }
        user.setRoles(resolveRoles(requested));

        User saved = userRepository.save(user);
        log.info("Registered user '{}' with roles {}{}", saved.getUsername(), requested,
                bootstrap ? " (bootstrap administrator)" : "");
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String hash = tokenProvider.hashRefreshToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new TokenRefreshException("Refresh token is not recognised"));

        if (stored.isRevoked()) {
            throw new TokenRefreshException("Refresh token has been revoked. Please log in again.");
        }
        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new TokenRefreshException("Refresh token has expired. Please log in again.");
        }

        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw new TokenRefreshException("This account has been disabled");
        }

        // Rotation: the old token is retired the moment it is used, so a stolen copy is
        // worth at most one use and a replay of an already-used token is detectable.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String accessToken = tokenProvider.generateAccessToken(UserPrincipal.from(user));
        String newRefreshToken = issueRefreshToken(user);

        return AuthResponse.of(accessToken, newRefreshToken,
                tokenProvider.getAccessExpirationSeconds(), userMapper.toResponse(user));
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        String hash = tokenProvider.hashRefreshToken(request.refreshToken());
        // An unknown token is treated as a successful logout on purpose: replying
        // "no such token" would tell an attacker which tokens are real.
        refreshTokenRepository.findByTokenHash(hash).ifPresent(stored -> {
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId());
            log.info("User '{}' logged out", stored.getUser().getUsername());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("The new password must differ from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // A password change must end every existing session, otherwise a session opened
        // with the old password would survive it.
        refreshTokenRepository.revokeAllForUser(userId);
        log.info("Password changed for user '{}'", user.getUsername());
    }

    // ------------------------------------------------------------------ helpers

    private String issueRefreshToken(User user) {
        String raw = tokenProvider.generateRefreshTokenValue();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(tokenProvider.hashRefreshToken(raw));   // only the hash is stored
        token.setExpiresAt(OffsetDateTime.now()
                .plusNanos(tokenProvider.getRefreshExpirationMs() * 1_000_000));
        refreshTokenRepository.save(token);
        return raw;   // the raw value is returned once and is never persisted
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        Set<Role> roles = new HashSet<>();
        for (RoleName name : names) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", name)));
        }
        return roles;
    }
}
