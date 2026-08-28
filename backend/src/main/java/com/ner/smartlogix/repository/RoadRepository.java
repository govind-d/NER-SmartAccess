package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Road;
import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.enums.RoadStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ner.smartlogix.repository.projection.RoadGeoRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The most important repository in the project: it carries the spatial queries that
 * connect incidents, routes and vehicles to the monitored road network.
 */
public interface RoadRepository extends JpaRepository<Road, Long>,
        JpaSpecificationExecutor<Road> {

    Optional<Road> findByCode(String code);

    List<Road> findByStatus(RoadStatus status);

    List<Road> findByCurrentRiskLevel(RiskLevel riskLevel);

    Page<Road> findByDistrictId(Long districtId, Pageable pageable);

    long countByStatus(RoadStatus status);

    long countByDistrictIdAndStatus(Long districtId, RoadStatus status);

    /**
     * Roads within N metres of a point - used to decide which road an incident blocks.
     *
     * <p>Casting to {@code geography} makes PostGIS measure real distance on the curved
     * earth in metres. Without the cast the number would be in degrees, which is
     * meaningless as a distance.
     */
    @Query(value = """
            SELECT * FROM road r
            WHERE ST_DWithin(r.geom::geography,
                             ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                             :radiusMeters)
            ORDER BY r.geom <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
            """, nativeQuery = true)
    List<Road> findNearPoint(@Param("lat") double lat,
                             @Param("lon") double lon,
                             @Param("radiusMeters") double radiusMeters);

    /** The single closest road to a point, or empty if none is within the radius. */
    @Query(value = """
            SELECT * FROM road r
            WHERE ST_DWithin(r.geom::geography,
                             ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                             :radiusMeters)
            ORDER BY r.geom <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
            LIMIT 1
            """, nativeQuery = true)
    Optional<Road> findNearestRoad(@Param("lat") double lat,
                                   @Param("lon") double lon,
                                   @Param("radiusMeters") double radiusMeters);

    /**
     * Which monitored roads does an externally computed route pass through?
     * This is how an OSRM polyline gets converted into our own risk picture (Phase 7).
     */
    @Query(value = """
            SELECT DISTINCT r.* FROM road r
            WHERE ST_DWithin(r.geom::geography,
                             ST_GeomFromText(:routeWkt, 4326)::geography,
                             :toleranceMeters)
            """, nativeQuery = true)
    List<Road> findAlongRoute(@Param("routeWkt") String routeWkt,
                              @Param("toleranceMeters") double toleranceMeters);

    /** Dashboard counters in one round trip instead of four. */
    @Query("SELECT r.status, COUNT(r) FROM Road r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();

    // ---------------------------------------------------------------- map layers

    /**
     * Roads as GeoJSON, ready for Leaflet. PostGIS encodes the geometry itself with
     * ST_AsGeoJSON, so no geometry ever has to be parsed in Java.
     *
     * <p>The {@code CAST(:status AS text)} is not decoration: PostgreSQL cannot infer the
     * type of a bare parameter used in {@code ? IS NULL}, and the query fails without it.
     */
    @Query(value = """
            SELECT r.id AS "id", r.code AS "code", r.name AS "name",
                   r.status AS "status", r.current_risk_level AS "riskLevel",
                   r.length_km AS "lengthKm", ST_AsGeoJSON(r.geom) AS "geojson"
            FROM road r
            WHERE (CAST(:status AS text) IS NULL OR r.status = CAST(:status AS text))
            """, nativeQuery = true)
    List<RoadGeoRow> findGeoJson(@Param("status") String status);

    /** Lets PostGIS measure the road on the curved earth so length can never drift. */
    @Query(value = "SELECT ST_Length(ST_GeomFromText(:wkt, 4326)::geography) / 1000",
           nativeQuery = true)
    Double computeLengthKm(@Param("wkt") String wkt);
}
