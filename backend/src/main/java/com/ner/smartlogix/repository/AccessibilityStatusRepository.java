package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.AccessibilityStatus;
import com.ner.smartlogix.enums.AccessibilityLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccessibilityStatusRepository extends JpaRepository<AccessibilityStatus, Long> {

    Optional<AccessibilityStatus> findFirstByDistrictIdOrderByEvaluatedAtDesc(Long districtId);

    /**
     * The newest snapshot for every district, in one query - the feed behind the
     * district-wise accessibility panel and the choropleth map layer.
     */
    @Query("""
           SELECT a FROM AccessibilityStatus a
           WHERE a.evaluatedAt = (
               SELECT MAX(a2.evaluatedAt) FROM AccessibilityStatus a2
               WHERE a2.district.id = a.district.id)
           """)
    List<AccessibilityStatus> findLatestForAllDistricts();

    long countByAccessibilityLevel(AccessibilityLevel level);
}
