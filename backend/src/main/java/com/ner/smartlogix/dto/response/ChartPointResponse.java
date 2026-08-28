package com.ner.smartlogix.dto.response;

/**
 * A single point on any chart: one label, one number.
 *
 * <p>Deliberately generic. Every chart on the dashboard - deliveries per day, incidents
 * by type, road status distribution - is a list of these, so the React side needs one
 * chart component rather than six.
 */
public record ChartPointResponse(String label, double value) {
}
