package com.ner.smartlogix.security;

import com.ner.smartlogix.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Creates and verifies tokens. The only class in the project that knows what a JWT
 * looks like - everything else just calls these methods.
 *
 * <p>Two very different kinds of token are produced here:
 * <ul>
 *   <li><b>Access token</b> - a signed JWT carrying the username, the user id and the
 *       roles. It is <em>self-contained</em>: verifying it needs no database call, which
 *       is what makes the API stateless and horizontally scalable.</li>
 *   <li><b>Refresh token</b> - NOT a JWT, just 64 random bytes. It carries no
 *       information, so it cannot be read or forged; it is only ever compared against a
 *       stored hash. It has to be revocable at logout, and a JWT cannot be un-issued.</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        // The key is derived once at startup. Its length decides the HMAC algorithm
        // (HS256 / HS384 / HS512) that jjwt selects automatically when signing.
        this.signingKey = Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ access token

    public String generateAccessToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getAccessExpirationMs());
        List<String> roles = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_USER_ID, principal.getId())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Object value = parseClaims(token).get(CLAIM_USER_ID);
        return value == null ? null : Long.valueOf(value.toString());
    }

    /**
     * Returns true only for a token that is well formed, correctly signed and unexpired.
     * Every failure is logged at DEBUG and swallowed: an invalid token is a normal event
     * (an expired session), not an application error.
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Expired JWT: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
        }
        return false;
    }

    public long getAccessExpirationSeconds() {
        return properties.getAccessExpirationMs() / 1000;
    }

    public long getRefreshExpirationMs() {
        return properties.getRefreshExpirationMs();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ----------------------------------------------------------------- refresh token

    /** 64 cryptographically random bytes, URL-safe encoded. Given to the client once. */
    public String generateRefreshTokenValue() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 of the refresh token. Only this hash is stored, so a leaked database dump
     * cannot be replayed as a valid session - the same reasoning as password hashing.
     * BCrypt is not used here because the value is already high-entropy random and the
     * lookup must be a fast, indexable equality match.
     */
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", ex);
        }
    }
}
