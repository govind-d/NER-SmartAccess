package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.DistrictRequest;
import com.ner.smartlogix.dto.response.AccessibilityResponse;
import com.ner.smartlogix.dto.response.DistrictResponse;
import java.util.List;

public interface DistrictService {

    List<DistrictResponse> findAll(String state);

    DistrictResponse findByCode(String code);

    DistrictResponse create(DistrictRequest request);

    DistrictResponse update(Long id, DistrictRequest request);

    void delete(Long id);

    AccessibilityResponse currentAccessibility(String districtCode);

    /** Newest snapshot for every district - the dashboard matrix. */
    List<AccessibilityResponse> accessibilitySummary();

    /**
     * Recomputes accessibility for one district from the current status of its roads and
     * stores a new snapshot. Called after any road status change.
     */
    AccessibilityResponse evaluateAccessibility(Long districtId);
}
