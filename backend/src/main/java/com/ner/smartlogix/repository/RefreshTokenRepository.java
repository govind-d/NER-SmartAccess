package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.RefreshToken;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Called on logout: kill every session of this user at once. */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId")
    int revokeAllForUser(@Param("userId") Long userId);

    /** Housekeeping job: remove tokens nobody can use any more. */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now OR t.revoked = true")
    int deleteExpiredAndRevoked(@Param("now") OffsetDateTime now);
}
