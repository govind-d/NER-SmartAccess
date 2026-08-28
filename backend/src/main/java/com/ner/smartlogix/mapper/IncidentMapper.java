package com.ner.smartlogix.mapper;

import com.ner.smartlogix.dto.response.FieldReportResponse;
import com.ner.smartlogix.dto.response.IncidentResponse;
import com.ner.smartlogix.entity.FieldReport;
import com.ner.smartlogix.entity.Incident;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

    /** The photo is exposed as a URL, never as a filesystem path. */
    public IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getClientUuid(),
                incident.getIncidentType(),
                incident.getSeverity(),
                incident.getDescription(),
                incident.getLatitude(),
                incident.getLongitude(),
                incident.getRoad() == null ? null : incident.getRoad().getCode(),
                incident.getRoad() == null ? null : incident.getRoad().getName(),
                incident.getDistrict() == null ? null : incident.getDistrict().getCode(),
                incident.getDistrict() == null ? null : incident.getDistrict().getName(),
                incident.getReportedBy() == null ? null : incident.getReportedBy().getFullName(),
                incident.getPhotoPath() == null
                        ? null : "/api/v1/incidents/" + incident.getId() + "/photo",
                incident.getStatus(),
                incident.getOccurredAt(),
                incident.getCreatedAt(),
                incident.getVerifiedAt());
    }

    public FieldReportResponse toResponse(FieldReport report) {
        return new FieldReportResponse(
                report.getId(),
                report.getClientUuid(),
                report.getReportType(),
                report.getDescription(),
                report.getLatitude(),
                report.getLongitude(),
                report.getReportedBy() == null ? null : report.getReportedBy().getFullName(),
                report.getSyncStatus(),
                report.getCapturedAt(),
                report.getSyncedAt(),
                report.getIncident() == null ? null : report.getIncident().getId());
    }
}
