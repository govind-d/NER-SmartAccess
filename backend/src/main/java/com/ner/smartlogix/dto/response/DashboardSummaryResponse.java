package com.ner.smartlogix.dto.response;

import java.util.List;

/**
 * Every summary card on the dashboard, in one response.
 *
 * <p>One aggregated endpoint instead of nine parallel requests: the first paint is faster,
 * the numbers are all from the same instant (so the cards can never contradict each
 * other), and it is trivial to cache later.
 */
public record DashboardSummaryResponse(
        long activeVehicles,
        long totalDeliveries,
        long inTransitDeliveries,
        long delayedDeliveries,
        long deliveredToday,
        long openRoads,
        long partiallyAccessibleRoads,
        long highRiskRoads,
        long blockedRoads,
        long activeIncidents,
        long criticalIncidents,
        long districtsCutOff,
        long districtsRestricted,
        List<AlertResponse> recentAlerts) {
}
