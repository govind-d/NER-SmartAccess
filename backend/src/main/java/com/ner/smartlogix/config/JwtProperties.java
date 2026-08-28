package com.ner.smartlogix.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code app.jwt.*} block of application.yml to a typed Java object, so the
 * rest of the code never repeats magic strings like {@code "${app.jwt.secret}"}.
 *
 * <p>The {@code @PostConstruct} check is deliberate: a short secret would still produce
 * working tokens but trivially forgeable ones. Failing at startup is far better than
 * discovering it in production.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;
    private long accessExpirationMs = 900_000L;      // 15 minutes
    private long refreshExpirationMs = 604_800_000L; // 7 days

    @PostConstruct
    public void validate() {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is missing or shorter than 32 bytes. "
                    + "Generate one with: openssl rand -base64 64");
        }
    }
}
