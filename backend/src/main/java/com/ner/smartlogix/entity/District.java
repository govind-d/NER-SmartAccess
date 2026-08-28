package com.ner.smartlogix.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;

/**
 * A district of the North Eastern Region - the unit at which accessibility,
 * weather and analytics are reported.
 *
 * <p>Two geometry columns are stored on purpose:
 * <ul>
 *   <li>{@code boundary} - the polygon, used for "which district contains this GPS
 *       point?" ({@code ST_Contains}) and for drawing the choropleth map</li>
 *   <li>{@code centroid} - a single point, used as a cheap origin/destination for
 *       routing and to place labels, so we never have to compute it at query time</li>
 * </ul>
 *
 * <p>SRID 4326 is plain latitude/longitude (WGS-84), the same system GPS devices and
 * Leaflet use, so no coordinate conversion is needed anywhere in the stack.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(name = "district")
public class District extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short stable key such as AS-KAM (Assam / Kamrup) used by the API and the UI. */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 60)
    private String state;

    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon boundary;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point centroid;

    private Integer population;

    @Column(name = "area_sq_km")
    private Double areaSqKm;
}
