package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.UpdateRolesRequest;
import com.ner.smartlogix.dto.request.UpdateUserRequest;
import com.ner.smartlogix.dto.response.RoleResponse;
import com.ner.smartlogix.dto.response.UserResponse;
import com.ner.smartlogix.entity.Role;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.RoleName;
import com.ner.smartlogix.exception.BusinessRuleException;
import com.ner.smartlogix.exception.DuplicateResourceException;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.UserMapper;
import com.ner.smartlogix.repository.DistrictRepository;
import com.ner.smartlogix.repository.RefreshTokenRepository;
import com.ner.smartlogix.repository.RoleRepository;
import com.ner.smartlogix.repository.UserRepository;
import com.ner.smartlogix.repository.UserSpecifications;
import com.ner.smartlogix.security.SecurityUtils;
import com.ner.smartlogix.service.UserService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Administration of user accounts. Every method here is reachable only by an ADMIN. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DistrictRepository districtRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> search(RoleName role, Long districtId, String text,
                                     Pageable pageable) {
        // Filters are added only when supplied, so an omitted filter never reaches SQL.
        Specification<User> spec = UserSpecifications.all();
        if (role != null) {
            spec = spec.and(UserSpecifications.hasRole(role));
        }
        if (districtId != null) {
            spec = spec.and(UserSpecifications.inDistrict(districtId));
        }
        if (StringUtils.hasText(text)) {
            spec = spec.and(UserSpecifications.matchesText(text));
        }
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);

        // Uniqueness must be re-checked, but only when the value actually changed.
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPhone(request.phone());

        if (StringUtils.hasText(request.districtCode())) {
            user.setDistrict(districtRepository.findByCode(request.districtCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "District", "code", request.districtCode())));
        } else {
            user.setDistrict(null);
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        User user = findUser(id);
        if (!enabled) {
            guardAgainstSelfLockout(id, "disable your own account");
        }
        user.setEnabled(enabled);
        User saved = userRepository.save(user);

        if (!enabled) {
            // Disabling must take effect immediately: revoke the refresh tokens so the
            // account cannot renew its session once the access token expires.
            refreshTokenRepository.revokeAllForUser(id);
        }
        log.info("User '{}' {}", saved.getUsername(), enabled ? "enabled" : "disabled");
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateRoles(Long id, UpdateRolesRequest request) {
        User user = findUser(id);
        Set<RoleName> requested = new HashSet<>(request.roles());

        // An administrator must not be able to strip their own ADMIN role and leave the
        // system with nobody who can administer it.
        if (!requested.contains(RoleName.ADMIN)) {
            guardAgainstSelfLockout(id, "remove your own ADMIN role");
        }

        Set<Role> roles = new HashSet<>();
        for (RoleName name : requested) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", name)));
        }
        user.setRoles(roles);
        log.info("Roles of user '{}' set to {}", user.getUsername(), requested);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        guardAgainstSelfLockout(id, "delete your own account");
        // Deleting is offered for completeness, but disabling is almost always the right
        // choice: incidents, deliveries and audit rows still point at this user.
        userRepository.delete(user);
        log.warn("User '{}' deleted", user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private void guardAgainstSelfLockout(Long targetUserId, String action) {
        if (SecurityUtils.currentUserId().map(targetUserId::equals).orElse(false)) {
            throw new BusinessRuleException("You cannot " + action);
        }
    }
}
