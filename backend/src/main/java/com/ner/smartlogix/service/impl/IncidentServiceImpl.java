package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.IncidentRequest;
import com.ner.smartlogix.dto.response.IncidentResponse;
import com.ner.smartlogix.entity.Incident;
import com.ner.smartlogix.entity.Road;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.IncidentStatus;
import com.ner.smartlogix.enums.IncidentType;
import com.ner.smartlogix.enums.Severity;
import com.ner.smartlogix.event.IncidentReportedEvent;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.IncidentMapper;
import com.ner.smartlogix.repository.DistrictRepository;
import com.ner.smartlogix.repository.IncidentRepository;
import com.ner.smartlogix.repository.RoadRepository;
import com.ner.smartlogix.repository.UserRepository;
import com.ner.smartlogix.security.SecurityUtils;
import com.ner.smartlogix.service.FileStorageService;
import com.ner.smartlogix.service.IncidentService;
import com.ner.smartlogix.util.GeometryUtils;
import com.ner.smartlogix.websocket.RealtimeBroadcaster;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Reporting a disruption from the field.
 *
 * <p>{@link #report} is short on purpose. It persists the incident, works out which road
 * and district it belongs to, and publishes one event. Everything that follows - closing
 * the road, re-scoring its risk, raising the alert, re-routing affected deliveries - is
 * done by listeners in {@code IncidentImpactListener}. Adding a new consequence never
 * means editing this method.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    /** How far from a reported point we still consider a road to be affected. */
    private static final double ROAD_MATCH_RADIUS_METERS = 500;

    private final IncidentRepository incidentRepository;
    private final RoadRepository roadRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final IncidentMapper incidentMapper;
    private final FileStorageService fileStorageService;
    private final RealtimeBroadcaster broadcaster;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentResponse> search(IncidentType type, Severity severity,
                                         IncidentStatus status, Long districtId,
                                         Pageable pageable) {
        Specification<Incident> spec = (root, query, cb) -> cb.conjunction();
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("incidentType"), type));
        }
        if (severity != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (districtId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("district").get("id"), districtId));
        }
        return incidentRepository.findAll(spec, pageable).map(incidentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse findById(Long id) {
        return incidentMapper.toResponse(requireIncident(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentResponse> findNear(double latitude, double longitude,
                                           double radiusMeters) {
        return incidentRepository.findNearPoint(latitude, longitude, radiusMeters).stream()
                .map(incidentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public IncidentResponse report(IncidentRequest request, MultipartFile photo) {
        // Idempotency. A phone that syncs the same queued report twice must not create
        // two landslides on the map, so we simply return what already exists.
        if (request.clientUuid() != null) {
            var existing = incidentRepository.findByClientUuid(request.clientUuid());
            if (existing.isPresent()) {
                log.debug("Duplicate sync of incident {} ignored", request.clientUuid());
                return incidentMapper.toResponse(existing.get());
            }
        }

        User reporter = currentUser();

        Incident incident = new Incident();
        incident.setClientUuid(request.clientUuid());
        incident.setIncidentType(request.incidentType());
        incident.setSeverity(request.severity());
        incident.setDescription(request.description());
        incident.setLatitude(request.latitude());
        incident.setLongitude(request.longitude());
        incident.setLocation(GeometryUtils.point(request.latitude(), request.longitude()));
        incident.setReportedBy(reporter);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setOccurredAt(request.occurredAt());

        // PostGIS decides which road this point belongs to - the reporter does not have
        // to know road codes, they just stand where the problem is.
        roadRepository.findNearestRoad(request.latitude(), request.longitude(),
                        ROAD_MATCH_RADIUS_METERS)
                .ifPresent(road -> {
                    incident.setRoad(road);
                    incident.setDistrict(road.getDistrict());
                });
        if (incident.getDistrict() == null) {
            districtRepository.findContainingPoint(request.latitude(), request.longitude())
                    .or(() -> districtRepository.findNearestByCentroid(
                            request.latitude(), request.longitude()))
                    .ifPresent(incident::setDistrict);
        }

        if (photo != null && !photo.isEmpty()) {
            incident.setPhotoPath(fileStorageService.store(photo, "incident"));
        }

        Incident saved = incidentRepository.save(incident);
        log.info("Incident #{} reported: {} ({}) by {}", saved.getId(),
                saved.getIncidentType(), saved.getSeverity(), reporter.getUsername());

        IncidentResponse response = incidentMapper.toResponse(saved);
        broadcaster.incidentReported(response);
        eventPublisher.publishEvent(new IncidentReportedEvent(saved.getId()));
        return response;
    }

    @Override
    @Transactional
    public IncidentResponse verify(Long id) {
        Incident incident = requireIncident(id);
        incident.setStatus(IncidentStatus.VERIFIED);
        incident.setVerifiedAt(OffsetDateTime.now());
        incident.setVerifiedBy(currentUser());
        return incidentMapper.toResponse(incidentRepository.save(incident));
    }

    @Override
    @Transactional
    public IncidentResponse changeStatus(Long id, IncidentStatus status) {
        Incident incident = requireIncident(id);
        incident.setStatus(status);
        if (status == IncidentStatus.VERIFIED && incident.getVerifiedAt() == null) {
            incident.setVerifiedAt(OffsetDateTime.now());
            incident.setVerifiedBy(currentUser());
        }
        Incident saved = incidentRepository.save(incident);
        broadcaster.incidentReported(incidentMapper.toResponse(saved));
        return incidentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        incidentRepository.delete(requireIncident(id));
    }

    /** Used by the controller to serve the stored photo. */
    @Transactional(readOnly = true)
    public String photoFileName(Long incidentId) {
        Incident incident = requireIncident(incidentId);
        if (incident.getPhotoPath() == null) {
            throw new ResourceNotFoundException("This incident has no photo");
        }
        return incident.getPhotoPath();
    }

    /** Exposed for the road-impact listener, which needs the entity rather than a DTO. */
    @Transactional(readOnly = true)
    public Road affectedRoad(Long incidentId) {
        return requireIncident(incidentId).getRoad();
    }

    private User currentUser() {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Incident requireIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));
    }
}
