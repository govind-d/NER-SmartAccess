package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.FieldReport;
import com.ner.smartlogix.enums.SyncStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldReportRepository extends JpaRepository<FieldReport, Long> {

    Optional<FieldReport> findByClientUuid(UUID clientUuid);

    boolean existsByClientUuid(UUID clientUuid);

    Page<FieldReport> findByReportedByIdOrderByCapturedAtDesc(Long userId, Pageable pageable);

    List<FieldReport> findBySyncStatus(SyncStatus syncStatus);

    /** Reports not yet promoted into a verified incident - the officials' review queue. */
    Page<FieldReport> findByIncidentIsNull(Pageable pageable);
}
