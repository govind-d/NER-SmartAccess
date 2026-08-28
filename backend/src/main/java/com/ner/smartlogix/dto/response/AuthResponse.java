package com.ner.smartlogix.dto.response;

/**
 * Result of a successful login or token refresh.
 *
 * <p>Two tokens with different jobs:
 * <ul>
 *   <li><b>accessToken</b> - a JWT, short-lived (15 min), sent on every request. It is
 *       self-contained, so the server never looks it up in the database.</li>
 *   <li><b>refreshToken</b> - a long random string, long-lived (7 days), sent only to
 *       {@code /auth/refresh}. Only its hash is stored, and it can be revoked at logout.</li>
 * </ul>
 *
 * <p>Short access token + revocable refresh token is the standard compromise: a stolen
 * access token is useless within minutes, and logout can genuinely end a session even
 * though JWTs themselves cannot be un-issued.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {

    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
