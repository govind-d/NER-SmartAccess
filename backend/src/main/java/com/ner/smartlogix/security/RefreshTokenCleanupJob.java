package com.ner.smartlogix.security;

import com.ner.smartlogix.repository.RefreshTokenRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Housekeeping. Expired and revoked refresh tokens are of no further use, and the table
 * would otherwise grow by one row per login for the life of the system.
 *
 * <p>Runs hourly. {@code @EnableScheduling} is already switched on in the application
 * class, which is what makes {@code @Scheduled} take effect.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 15 * * * *")   // at minute 15 of every hour
    @Transactional
    public void purgeDeadTokens() {
        int removed = refreshTokenRepository.deleteExpiredAndRevoked(OffsetDateTime.now());
        if (removed > 0) {
            log.debug("Purged {} expired or revoked refresh tokens", removed);
        }
    }
}
