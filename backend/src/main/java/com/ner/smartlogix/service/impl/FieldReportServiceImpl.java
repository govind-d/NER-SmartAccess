package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.FieldReportSyncRequest;
import com.ner.smartlogix.dto.request.IncidentRequest;
import com.ner.smartlogix.dto.response.FieldReportResponse;
import com.ner.smartlogix.dto.response.SyncResultResponse;
import com.ner.smartlogix.entity.FieldReport;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.Severity;
import com.ner.smartlogix.enums.SyncStatus;
import com.ner.smartlogix.exception.BusinessRuleException;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.IncidentMapper;
import com.ner.smartlogix.repository.FieldReportRepository;
import com.ner.smartlogix.repository.IncidentRepository;
import com.ner.smartlogix.repository.UserRepository;
import com.ner.smartlogix.security.SecurityUtils;
import com.ner.smartlogix.service.FieldReportService;
import com.ner.smartlogix.service.IncidentService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The server side of offline field reporting.
 *
 * <p>The whole design turns on two ideas:
 * <ul>
 *   <li><b>Idempotency.</b> The phone generates a UUID before saving the form. Sending it
 *       twice is harmless, so the phone can retry as often as the network requires.</li>
 *   <li><b>Per-item results.</b> A batch of ten reports never fails as a unit. Each item
 *       comes back accepted, duplicate or failed, and the phone clears exactly the rows
 *       that reached us.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldReportServiceImpl implements FieldReportService {

    private final FieldReportRepository fieldReportRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final IncidentService incidentService;
    private final IncidentMapper incidentMapper;

    @Override
    @Transactional
    public SyncResultResponse sync(FieldReportSyncRequest request) {
        User reporter = currentUser();
        List<UUID> accepted = new ArrayList<>();
        List<UUID> duplicates = new ArrayList<>();
        List<SyncResultResponse.Failure> failed = new ArrayList<>();

        for (FieldReportSyncRequest.Item item : request.reports()) {
            try {
                if (fieldReportRepository.existsByClientUuid(item.clientUuid())) {
                    duplicates.add(item.clientUuid());
                    continue;
                }
                FieldReport report = new FieldReport();
                report.setClientUuid(item.clientUuid());
                report.setReportedBy(reporter);
                report.setReportType(item.reportType());
                report.setDescription(item.description());
                report.setLatitude(item.latitude());
                report.setLongitude(item.longitude());
                report.setSyncStatus(SyncStatus.SYNCED);
                report.setCapturedAt(item.capturedAt());   // when the officer typed it
                report.setSyncedAt(OffsetDateTime.now());  // when we received it
                fieldReportRepository.save(report);
                accepted.add(item.clientUuid());
            } catch (Exception ex) {
                // One bad row must never cost the other nine.
                log.warn("Field report {} rejected: {}", item.clientUuid(), ex.getMessage());
                failed.add(new SyncResultResponse.Failure(item.clientUuid(), ex.getMessage()));
            }
        }

        log.info("Offline sync by {}: {} accepted, {} duplicates, {} failed",
                reporter.getUsername(), accepted.size(), duplicates.size(), failed.size());
        return new SyncResultResponse(accepted, duplicates, failed);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FieldReportResponse> findMine(Pageable pageable) {
        return fieldReportRepository
                .findByReportedByIdOrderByCapturedAtDesc(currentUser().getId(), pageable)
                .map(incidentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FieldReportResponse> findPendingReview(Pageable pageable) {
        return fieldReportRepository.findByIncidentIsNull(pageable)
                .map(incidentMapper::toResponse);
    }

    /**
     * An official has reviewed the report and believes it. Promoting it creates a real
     * incident, which is what actually closes roads and raises alerts - a raw report on
     * its own never changes the operational picture.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public FieldReportResponse promoteToIncident(Long reportId) {
        FieldReport report = fieldReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("FieldReport", "id", reportId));
        if (report.getIncident() != null) {
            throw new BusinessRuleException("This report has already become incident #"
                    + report.getIncident().getId());
        }

        var response = incidentService.report(new IncidentRequest(
                UUID.randomUUID(),
                report.getReportType(),
                Severity.HIGH,          // a report an official chose to promote is serious
                report.getDescription(),
                report.getLatitude(),
                report.getLongitude(),
                report.getCapturedAt()), null);

        incidentRepository.findById(response.id()).ifPresent(report::setIncident);
        return incidentMapper.toResponse(fieldReportRepository.save(report));
    }

    private User currentUser() {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
