package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.IncidentRequest;
import com.ner.smartlogix.dto.response.IncidentResponse;
import com.ner.smartlogix.enums.IncidentStatus;
import com.ner.smartlogix.enums.IncidentType;
import com.ner.smartlogix.enums.Severity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface IncidentService {

    Page<IncidentResponse> search(IncidentType type, Severity severity, IncidentStatus status,
                                  Long districtId, Pageable pageable);

    IncidentResponse findById(Long id);

    List<IncidentResponse> findNear(double latitude, double longitude, double radiusMeters);

    /** Creates an incident and triggers the whole downstream chain. Idempotent on clientUuid. */
    IncidentResponse report(IncidentRequest request, MultipartFile photo);

    IncidentResponse verify(Long id);

    IncidentResponse changeStatus(Long id, IncidentStatus status);

    void delete(Long id);
}
