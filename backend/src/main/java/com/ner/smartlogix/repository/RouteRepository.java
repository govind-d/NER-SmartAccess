package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Route;
import com.ner.smartlogix.enums.RiskLevel;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByParentRouteId(Long parentRouteId);

    List<Route> findByRiskLevel(RiskLevel riskLevel);

    long countByRiskLevel(RiskLevel riskLevel);

    /**
     * {@code @EntityGraph} loads the segments in the SAME query instead of firing one
     * extra query per segment. This is the standard cure for the N+1 problem.
     */
    @EntityGraph(attributePaths = {"segments", "segments.road"})
    Optional<Route> findWithSegmentsById(Long id);
}
