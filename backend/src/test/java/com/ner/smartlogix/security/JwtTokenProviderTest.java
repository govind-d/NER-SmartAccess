package com.ner.smartlogix.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ner.smartlogix.config.JwtProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Unit tests for token creation and verification.
 *
 * <p>No Spring context and no database: this class exercises pure Java, so it runs in
 * milliseconds. That is the pay-off for keeping JWT logic in one small class instead of
 * scattering it through the filter and the service.
 */
class JwtTokenProviderTest {

    private static final String SECRET =
            "test-secret-that-is-long-enough-for-hmac-sha-algorithms-0123456789";

    private JwtTokenProvider tokenProvider;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessExpirationMs(900_000L);
        properties.setRefreshExpirationMs(604_800_000L);

        tokenProvider = new JwtTokenProvider(properties);
        principal = new UserPrincipal(42L, "officer1", "irrelevant-hash", "R. Marak", true,
                List.of(new SimpleGrantedAuthority("ROLE_FIELD_OFFICER")));
    }

    @Test
    @DisplayName("A freshly issued token is valid and carries the username and user id")
    void generatesReadableValidToken() {
        String token = tokenProvider.generateAccessToken(principal);

        assertThat(tokenProvider.isValid(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("officer1");
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("A token signed with a different secret is rejected")
    void rejectsForeignSignature() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("a-completely-different-secret-value-0123456789-abcdefgh");
        String foreignToken = new JwtTokenProvider(otherProperties).generateAccessToken(principal);

        assertThat(tokenProvider.isValid(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("An expired token is rejected")
    void rejectsExpiredToken() {
        JwtProperties expiring = new JwtProperties();
        expiring.setSecret(SECRET);
        expiring.setAccessExpirationMs(-1_000L);   // already in the past

        String token = new JwtTokenProvider(expiring).generateAccessToken(principal);
        assertThat(tokenProvider.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("Garbage is rejected instead of throwing")
    void rejectsMalformedToken() {
        assertThat(tokenProvider.isValid("not.a.token")).isFalse();
        assertThat(tokenProvider.isValid("")).isFalse();
    }

    @Test
    @DisplayName("Refresh tokens are random, and their hash is stable")
    void refreshTokensAreRandomAndHashConsistently() {
        String first = tokenProvider.generateRefreshTokenValue();
        String second = tokenProvider.generateRefreshTokenValue();

        assertThat(first).isNotEqualTo(second);
        assertThat(tokenProvider.hashRefreshToken(first))
                .isEqualTo(tokenProvider.hashRefreshToken(first))
                .isNotEqualTo(tokenProvider.hashRefreshToken(second))
                .isNotEqualTo(first);   // the stored hash never equals the raw token
    }

    @Test
    @DisplayName("A secret shorter than 32 bytes stops the application at startup")
    void rejectsWeakSecret() {
        JwtProperties weak = new JwtProperties();
        weak.setSecret("too-short");

        assertThatThrownBy(weak::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
