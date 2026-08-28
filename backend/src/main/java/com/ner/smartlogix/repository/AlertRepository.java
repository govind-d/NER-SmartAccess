package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Alert;
import com.ner.smartlogix.enums.AlertType;
import com.ner.smartlogix.enums.Severity;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<Alert, Long>,
        JpaSpecificationExecutor<Alert> {

    Page<Alert> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    List<Alert> findTop10ByActiveTrueOrderByCreatedAtDesc();

    List<Alert> findByActiveTrueAndDistrictId(Long districtId);

    List<Alert> findByActiveTrueAndAlertTypeAndSeverity(AlertType type, Severity severity);

    long countByActiveTrue();

    /** Scheduled cleanup: alerts are switched off, never deleted - history is evidence. */
    @Modifying
    @Query("UPDATE Alert a SET a.active = false WHERE a.expiresAt IS NOT NULL AND a.expiresAt < :now AND a.active = true")
    int deactivateExpired(@Param("now") OffsetDateTime now);
}
