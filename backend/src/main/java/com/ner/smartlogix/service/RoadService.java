package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.RoadRequest;
import com.ner.smartlogix.dto.request.RoadStatusRequest;
import com.ner.smartlogix.dto.response.RoadResponse;
import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.enums.RoadStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoadService {

    Page<RoadResponse> search(Long districtId, RoadStatus status, RiskLevel riskLevel,
                              Pageable pageable);

    RoadResponse findById(Long id);

    RoadResponse findByCode(String code);

    List<RoadResponse> findByStatus(RoadStatus status);

    List<RoadResponse> findNear(double latitude, double longitude, double radiusMeters);

    RoadResponse create(RoadRequest request);

    RoadResponse update(Long id, RoadRequest request);

    /** Changes status, stamps the time and raises an alert. Used by officials and by
     *  the incident listener. */
    RoadResponse changeStatus(Long id, RoadStatusRequest request);

    void delete(Long id);
}
