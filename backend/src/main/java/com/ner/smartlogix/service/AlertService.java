package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.AlertRequest;
import com.ner.smartlogix.dto.response.AlertResponse;
import com.ner.smartlogix.entity.*;
import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.enums.Severity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Raising and reading alerts.
 *
 * <p>The typed {@code raise*} methods exist so that callers never have to remember which
 * AlertType and Severity belong to which situation - the mapping from "a landslide was
 * reported" to "CRITICAL LANDSLIDE_RISK alert on this road in this district" lives here,
 * once.
 */
public interface AlertService {

    Page<AlertResponse> search(Boolean active, Severity severity, Long districtId,
                               Pageable pageable);

    List<AlertResponse> recent(int limit);

    AlertResponse createManual(AlertRequest request);

    void deactivate(Long id);

    // ---- system-raised alerts ----

    AlertResponse raiseIncidentAlert(Incident incident);

    AlertResponse raiseRoadStatusAlert(Road road, String reason);

    AlertResponse raiseRiskAlert(Road road, RiskLevel newLevel, String explanation);

    AlertResponse raiseWeatherAlert(District district, WeatherData weather);

    AlertResponse raiseDeliveryDelayAlert(Delivery delivery, int delayMinutes, String reason);

    AlertResponse raiseDangerZoneAlert(Vehicle vehicle, Road road);

    AlertResponse raiseDistrictInaccessibleAlert(District district);
}
