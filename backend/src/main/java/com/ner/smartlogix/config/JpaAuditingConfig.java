package com.ner.smartlogix.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Turns on JPA auditing so that {@code created_at}, {@code updated_at},
 * {@code created_by} and {@code updated_by} are filled in automatically
 * for every entity that extends {@link com.ner.smartlogix.entity.BaseAuditEntity}.
 *
 * <p>Without the {@link AuditorAware} bean below, Spring would know <em>when</em>
 * a row changed but not <em>who</em> changed it.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware",
                   dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    /**
     * Supplies the timestamp for {@code @CreatedDate} and {@code @LastModifiedDate}.
     *
     * <p>Without this bean Spring Data uses its default provider, which returns a
     * {@link java.time.LocalDateTime}, and auditing then fails at runtime with
     * "Cannot convert unsupported date type LocalDateTime to OffsetDateTime".
     *
     * <p>The audit fields are deliberately {@code OffsetDateTime} because the columns are
     * {@code TIMESTAMPTZ}: a landslide reported at 09:14 in Shillong and one reported at
     * 09:14 UTC are not the same moment, and dropping the offset would lose that.
     */
    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    /** Returns the username of the logged-in user, or "system" for background jobs. */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.of(auth.getName());
        };
    }
}
