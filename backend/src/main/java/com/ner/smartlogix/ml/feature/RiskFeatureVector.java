package com.ner.smartlogix.ml.feature;

import com.ner.smartlogix.enums.RoadCondition;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.enums.WeatherCondition;

/**
 * Everything the risk engine is allowed to look at, in one immutable object.
 *
 * <p>This record is the most important design decision in the AI module. Both the
 * rule engine written now and the Tribuo model added later consume exactly this type, so
 * the feature extraction code is written once and reused by both, and the two can be run
 * side by side on identical inputs to compare their answers.
 *
 * @param roadId                  which road this describes
 * @param rainfall24h             millimetres in the last day - the flash-flood signal
 * @param rainfall72h             millimetres over three days - the slope-saturation signal
 * @param weatherCondition        current normalised weather
 * @param terrainSlopeDegrees     average slope; steep hill roads fail far sooner in rain
 * @param landslideSusceptibility 0 to 1, from geological zonation
 * @param floodProne              does this stretch flood
 * @param roadCondition           surface condition
 * @param roadStatus              current operational status
 * @param incidentCount30d        how often this road has failed recently
 * @param daysSinceLastIncident   999 when there has never been one
 * @param historicalBlockDaysPerYear how many days a year it is normally unusable
 * @param trafficCongestionLevel  0 (clear) to 4 (jammed)
 * @param monsoonMonth            June to September in the NER
 * @param worstBridgeCondition    the weakest bridge on this road, if any
 */
public record RiskFeatureVector(
        Long roadId,
        double rainfall24h,
        double rainfall72h,
        WeatherCondition weatherCondition,
        double terrainSlopeDegrees,
        double landslideSusceptibility,
        boolean floodProne,
        RoadCondition roadCondition,
        RoadStatus roadStatus,
        int incidentCount30d,
        int daysSinceLastIncident,
        double historicalBlockDaysPerYear,
        int trafficCongestionLevel,
        boolean monsoonMonth,
        RoadCondition worstBridgeCondition) {

    /**
     * The same features as a plain array, in a fixed order.
     *
     * <p>Nothing uses this yet. It exists because Tribuo and Weka both want numeric rows,
     * and having the ordering defined next to the features themselves is what will make
     * the Phase 8 migration a small change rather than a rewrite.
     */
    public double[] toNumericArray() {
        return new double[]{
                rainfall24h,
                rainfall72h,
                terrainSlopeDegrees,
                landslideSusceptibility,
                floodProne ? 1 : 0,
                ordinalOf(roadCondition),
                incidentCount30d,
                Math.min(daysSinceLastIncident, 365),
                historicalBlockDaysPerYear,
                trafficCongestionLevel,
                monsoonMonth ? 1 : 0
        };
    }

    public static String[] featureNames() {
        return new String[]{
                "rainfall24h", "rainfall72h", "slopeDegrees", "landslideSusceptibility",
                "floodProne", "roadCondition", "incidentCount30d", "daysSinceLastIncident",
                "historicalBlockDaysPerYear", "trafficCongestion", "monsoonMonth"
        };
    }

    private double ordinalOf(RoadCondition condition) {
        return condition == null ? 0 : condition.ordinal();
    }
}
