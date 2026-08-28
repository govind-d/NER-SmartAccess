package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.RoadRiskAssessment;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadRiskAssessmentRepository extends JpaRepository<RoadRiskAssessment, Long> {

    Optional<RoadRiskAssessment> findFirstByRoadIdOrderByAssessedAtDesc(Long roadId);

    List<RoadRiskAssessment> findByRoadIdAndAssessedAtAfterOrderByAssessedAtAsc(
            Long roadId, OffsetDateTime after);

    /** Phase 8: the accumulated history becomes the ML training set. */
    List<RoadRiskAssessment> findByAssessedAtAfter(OffsetDateTime after);
}
