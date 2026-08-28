package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.*;
import com.ner.smartlogix.dto.response.AuthResponse;
import com.ner.smartlogix.dto.response.UserResponse;

/**
 * Everything to do with proving who you are.
 *
 * <p>The layer is defined as an interface and implemented separately so that controllers
 * depend on the contract, not the implementation - which is what makes the service easy
 * to mock in a unit test and easy to replace later (SOLID: dependency inversion).
 */
public interface AuthService {

    /** Verifies credentials and issues an access token plus a refresh token. */
    AuthResponse login(LoginRequest request);

    /**
     * Creates a user. Restricted to ADMIN, with one exception: if the database contains
     * no users at all, the first registration bootstraps the system and becomes ADMIN.
     */
    UserResponse register(RegisterRequest request);

    /** Exchanges a valid refresh token for a new access token, rotating the refresh token. */
    AuthResponse refresh(RefreshTokenRequest request);

    /** Revokes every refresh token of the owner of the supplied token. */
    void logout(RefreshTokenRequest request);

    /** The profile of the caller, taken from the SecurityContext. */
    UserResponse currentUser();

    /** Changes the caller's own password and ends all their other sessions. */
    void changePassword(ChangePasswordRequest request);
}
