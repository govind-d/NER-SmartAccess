package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Incident;
import com.ner.smartlogix.repository.projection.IncidentGeoRow;
import com.ner.smartlogix.enums.IncidentStatus;
import com.ner.smartlogix.enums.IncidentType;
import com.ner.smartlogix.enums.Severity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long>,
        JpaSpecificationExecutor<Incident> {

    /** The idempotency check that makes offline re-sync safe. */
    Optional<Incident> findByClientUuid(UUID clientUuid);

    boolean existsByClientUuid(UUID clientUuid);

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    List<Incident> findByRoadIdAndOccurredAtAfter(Long roadId, OffsetDateTime after);

    long countByStatusNot(IncidentStatus status);

    long countBySeverityAndStatusNot(Severity severity, IncidentStatus status);

    /** Feature of the risk engine: how often has this road failed recently? */
    @Query("""
           SELECT COUNT(i) FROM Incident i
           WHERE i.road.id = :roadId AND i.occurredAt >= :since
             AND i.status <> com.ner.smartlogix.enums.IncidentStatus.REJECTED
           """)
    long countRecentByRoad(@Param("roadId") Long roadId, @Param("since") OffsetDateTime since);

    /** Second feature: when did it last fail? */
    Optional<Incident> findFirstByRoadIdOrderByOccurredAtDesc(Long roadId);

    /** Map pins near a point. */
    @Query(value = """
            SELECT * FROM incident i
            WHERE ST_DWithin(i.location::geography,
                             ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                             :radiusMeters)
            ORDER BY i.occurred_at DESC
            """, nativeQuery = true)
    List<Incident> findNearPoint(@Param("lat") double lat,
                                 @Param("lon") double lon,
                                 @Param("radiusMeters") double radiusMeters);

    /** Analytics: incidents grouped by type over a period. */
    @Query("""
           SELECT i.incidentType, COUNT(i) FROM Incident i
           WHERE i.occurredAt >= :since GROUP BY i.incidentType
           """)
    List<Object[]> countGroupedByTypeSince(@Param("since") OffsetDateTime since);

    List<Incident> findByIncidentTypeAndOccurredAtAfter(IncidentType type, OffsetDateTime after);

    /** Incident pins for the map, optionally only the ones still open. */
    @Query(value = """
            SELECT i.id AS "id", i.incident_type AS "incidentType", i.severity AS "severity",
                   i.status AS "status", i.description AS "description",
                   TO_CHAR(i.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SSOF') AS "occurredAt",
                   ST_AsGeoJSON(i.location) AS "geojson"
            FROM incident i
            WHERE (:activeOnly = FALSE OR i.status NOT IN ('RESOLVED','REJECTED'))
            ORDER BY i.occurred_at DESC
            """, nativeQuery = true)
    List<IncidentGeoRow> findGeoJson(@Param("activeOnly") boolean activeOnly);
}
