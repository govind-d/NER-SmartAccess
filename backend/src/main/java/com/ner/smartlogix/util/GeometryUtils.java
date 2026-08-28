package com.ner.smartlogix.util;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTWriter;

/**
 * Builds JTS geometries for SRID 4326 (plain latitude/longitude).
 *
 * <p>The single most common bug in GIS code is coordinate order. JTS thinks in
 * {@code (x, y)}, which means {@code (longitude, latitude)} - the opposite of how people
 * say it aloud. Every conversion in this project goes through these methods so the swap
 * happens in exactly one place.
 */
public final class GeometryUtils {

    public static final int SRID = 4326;

    private static final GeometryFactory FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID);
    private static final WKTWriter WKT_WRITER = new WKTWriter();

    private GeometryUtils() {
    }

    public static Point point(double latitude, double longitude) {
        return FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    /** Builds a line from an ordered list of [latitude, longitude] pairs. */
    public static LineString lineString(List<double[]> latLonPairs) {
        if (latLonPairs == null || latLonPairs.size() < 2) {
            throw new IllegalArgumentException("A line needs at least two points");
        }
        Coordinate[] coordinates = latLonPairs.stream()
                .map(pair -> new Coordinate(pair[1], pair[0]))
                .toArray(Coordinate[]::new);
        return FACTORY.createLineString(coordinates);
    }

    /** Well-Known Text, the format PostGIS functions accept as a string parameter. */
    public static String toWkt(org.locationtech.jts.geom.Geometry geometry) {
        return WKT_WRITER.write(geometry);
    }

    /**
     * Straight-line distance in kilometres (haversine). Good enough for sanity checks
     * and ETA fallbacks; anything that must be accurate along a road asks PostGIS or the
     * routing engine instead.
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
