package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.CoordinateRequest;
import com.ner.smartlogix.dto.request.RoadRequest;
import com.ner.smartlogix.dto.request.RoadStatusRequest;
import com.ner.smartlogix.dto.response.RoadResponse;
import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.entity.Road;
import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.event.RoadStatusChangedEvent;
import com.ner.smartlogix.exception.DuplicateResourceException;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.RoadMapper;
import com.ner.smartlogix.repository.DistrictRepository;
import com.ner.smartlogix.repository.RoadRepository;
import com.ner.smartlogix.service.RoadService;
import com.ner.smartlogix.util.GeometryUtils;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.LineString;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Roads: the monitored network at the centre of the platform.
 *
 * <p>Two things here are worth studying:
 * <ul>
 *   <li>Road length is never taken from the request. The geometry is sent to PostGIS,
 *       which measures it on the curved earth, so the number and the line on the map can
 *       never disagree.</li>
 *   <li>{@link #changeStatus} does not send alerts or recompute district accessibility
 *       itself. It publishes {@link RoadStatusChangedEvent} and lets listeners react, so
 *       this class stays about roads and nothing else.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadServiceImpl implements RoadService {

    private final RoadRepository roadRepository;
    private final DistrictRepository districtRepository;
    private final RoadMapper roadMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<RoadResponse> search(Long districtId, RoadStatus status, RiskLevel riskLevel,
                                     Pageable pageable) {
        Specification<Road> spec = (root, query, cb) -> cb.conjunction();
        if (districtId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("district").get("id"), districtId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (riskLevel != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("currentRiskLevel"), riskLevel));
        }
        return roadRepository.findAll(spec, pageable).map(roadMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoadResponse findById(Long id) {
        return roadMapper.toResponse(requireRoad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public RoadResponse findByCode(String code) {
        return roadMapper.toResponse(roadRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Road", "code", code)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoadResponse> findByStatus(RoadStatus status) {
        return roadRepository.findByStatus(status).stream().map(roadMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoadResponse> findNear(double latitude, double longitude, double radiusMeters) {
        return roadRepository.findNearPoint(latitude, longitude, radiusMeters).stream()
                .map(roadMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoadResponse create(RoadRequest request) {
        if (roadRepository.findByCode(request.code()).isPresent()) {
            throw new DuplicateResourceException("Road", "code", request.code());
        }
        Road road = new Road();
        apply(road, request);
        road.setStatus(RoadStatus.OPEN);
        road.setCurrentRiskLevel(RiskLevel.LOW_RISK);
        road.setStatusUpdatedAt(OffsetDateTime.now());
        return roadMapper.toResponse(roadRepository.save(road));
    }

    @Override
    @Transactional
    public RoadResponse update(Long id, RoadRequest request) {
        Road road = requireRoad(id);
        if (!road.getCode().equals(request.code())
                && roadRepository.findByCode(request.code()).isPresent()) {
            throw new DuplicateResourceException("Road", "code", request.code());
        }
        apply(road, request);
        return roadMapper.toResponse(roadRepository.save(road));
    }

    @Override
    @Transactional
    public RoadResponse changeStatus(Long id, RoadStatusRequest request) {
        Road road = requireRoad(id);
        RoadStatus previous = road.getStatus();

        if (previous == request.status()) {
            return roadMapper.toResponse(road);   // nothing changed, raise nothing
        }

        road.setStatus(request.status());
        road.setStatusUpdatedAt(OffsetDateTime.now());
        Road saved = roadRepository.save(road);

        log.info("Road {} changed from {} to {} ({})", saved.getCode(), previous,
                request.status(), request.reason() == null ? "no reason given" : request.reason());

        eventPublisher.publishEvent(new RoadStatusChangedEvent(
                saved.getId(), saved.getCode(), saved.getName(), previous, request.status(),
                saved.getDistrict().getId(), request.reason()));

        return roadMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Road road = requireRoad(id);
        roadRepository.delete(road);
        log.warn("Road '{}' deleted", road.getCode());
    }

    private void apply(Road road, RoadRequest request) {
        District district = districtRepository.findByCode(request.districtCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "District", "code", request.districtCode()));

        LineString geometry = GeometryUtils.lineString(request.path().stream()
                .map(this::toLatLon)
                .toList());

        road.setCode(request.code());
        road.setName(request.name());
        road.setRoadType(request.roadType());
        road.setDistrict(district);
        road.setGeom(geometry);
        road.setCondition(request.condition());
        road.setSlopeDegrees(request.slopeDegrees() == null ? 0.0 : request.slopeDegrees());
        road.setLandslideSusceptibility(request.landslideSusceptibility() == null
                ? 0.0 : request.landslideSusceptibility());
        road.setFloodProne(request.floodProne());
        road.setHistoricalBlockDaysPerYear(request.historicalBlockDaysPerYear() == null
                ? 0.0 : request.historicalBlockDaysPerYear());

        // PostGIS measures the line; the client is never trusted with the length.
        Double lengthKm = roadRepository.computeLengthKm(GeometryUtils.toWkt(geometry));
        road.setLengthKm(lengthKm == null || lengthKm <= 0 ? 0.01 : lengthKm);
    }

    private double[] toLatLon(CoordinateRequest coordinate) {
        return new double[]{coordinate.latitude(), coordinate.longitude()};
    }

    private Road requireRoad(Long id) {
        return roadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Road", "id", id));
    }
}
