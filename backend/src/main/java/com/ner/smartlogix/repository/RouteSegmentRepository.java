package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.RouteSegment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {

    List<RouteSegment> findByRouteIdOrderBySequenceNoAsc(Long routeId);

    List<RouteSegment> findByRoadId(Long roadId);
}
