package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.repository.projection.DistrictGeoRow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DistrictRepository extends JpaRepository<District, Long> {

    Optional<District> findByCode(String code);

    List<District> findByStateIgnoreCase(String state);

    boolean existsByCode(String code);

    /**
     * Which district contains this GPS point? PostGIS answers it with ST_Contains,
     * using the GiST index on the boundary column.
     */
    @Query(value = """
            SELECT * FROM district d
            WHERE d.boundary IS NOT NULL
              AND ST_Contains(d.boundary, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326))
            LIMIT 1
            """, nativeQuery = true)
    Optional<District> findContainingPoint(@Param("lat") double lat, @Param("lon") double lon);

    /** Fallback when boundaries are approximate: nearest district centroid. */
    @Query(value = """
            SELECT * FROM district d
            WHERE d.centroid IS NOT NULL
            ORDER BY d.centroid <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
            LIMIT 1
            """, nativeQuery = true)
    Optional<District> findNearestByCentroid(@Param("lat") double lat, @Param("lon") double lon);

    /**
     * District boundaries with their newest accessibility level - the single request
     * behind the choropleth layer on the map.
     */
    @Query(value = """
            SELECT d.id AS "id", d.code AS "code", d.name AS "name", d.state AS "state",
                   COALESCE((SELECT a.accessibility_level FROM accessibility_status a
                             WHERE a.district_id = d.id
                             ORDER BY a.evaluated_at DESC LIMIT 1),
                            'FULLY_ACCESSIBLE') AS "accessibility",
                   ST_AsGeoJSON(d.boundary) AS "geojson"
            FROM district d
            WHERE d.boundary IS NOT NULL
            """, nativeQuery = true)
    List<DistrictGeoRow> findGeoJson();
}
