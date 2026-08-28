package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.DistrictRequest;
import com.ner.smartlogix.dto.response.AccessibilityResponse;
import com.ner.smartlogix.dto.response.DistrictResponse;
import com.ner.smartlogix.entity.AccessibilityStatus;
import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.enums.AccessibilityLevel;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.exception.DuplicateResourceException;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.DistrictMapper;
import com.ner.smartlogix.repository.AccessibilityStatusRepository;
import com.ner.smartlogix.repository.DistrictRepository;
import com.ner.smartlogix.repository.RoadRepository;
import com.ner.smartlogix.service.DistrictService;
import com.ner.smartlogix.util.GeometryUtils;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Districts and their accessibility.
 *
 * <p>The interesting method is {@link #evaluateAccessibility(Long)}: it turns the raw
 * status of individual roads into the single word a decision-maker actually needs -
 * is this district reachable or not.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistrictServiceImpl implements DistrictService {

    private final DistrictRepository districtRepository;
    private final RoadRepository roadRepository;
    private final AccessibilityStatusRepository accessibilityRepository;
    private final DistrictMapper districtMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DistrictResponse> findAll(String state) {
        List<District> districts = StringUtils.hasText(state)
                ? districtRepository.findByStateIgnoreCase(state)
                : districtRepository.findAll();

        // One query for every district's newest snapshot, then an in-memory lookup.
        // Calling the repository once per district would be the classic N+1 mistake.
        Map<Long, AccessibilityStatus> latestByDistrict =
                accessibilityRepository.findLatestForAllDistricts().stream()
                        .collect(Collectors.toMap(
                                status -> status.getDistrict().getId(),
                                Function.identity(),
                                (first, second) -> first));

        return districts.stream()
                .map(district -> districtMapper.toResponse(
                        district, latestByDistrict.get(district.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DistrictResponse findByCode(String code) {
        District district = requireByCode(code);
        return districtMapper.toResponse(district,
                accessibilityRepository
                        .findFirstByDistrictIdOrderByEvaluatedAtDesc(district.getId())
                        .orElse(null));
    }

    @Override
    @Transactional
    public DistrictResponse create(DistrictRequest request) {
        if (districtRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("District", "code", request.code());
        }
        District district = new District();
        apply(district, request);
        return districtMapper.toResponse(districtRepository.save(district), null);
    }

    @Override
    @Transactional
    public DistrictResponse update(Long id, DistrictRequest request) {
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", id));
        if (!district.getCode().equals(request.code())
                && districtRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("District", "code", request.code());
        }
        apply(district, request);
        return districtMapper.toResponse(districtRepository.save(district), null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", id));
        districtRepository.delete(district);
        log.warn("District '{}' deleted", district.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public AccessibilityResponse currentAccessibility(String districtCode) {
        District district = requireByCode(districtCode);
        return accessibilityRepository
                .findFirstByDistrictIdOrderByEvaluatedAtDesc(district.getId())
                .map(districtMapper::toAccessibilityResponse)
                .orElseGet(() -> evaluateAccessibility(district.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessibilityResponse> accessibilitySummary() {
        return accessibilityRepository.findLatestForAllDistricts().stream()
                .map(districtMapper::toAccessibilityResponse)
                .toList();
    }

    /**
     * The accessibility rule, in one place:
     *
     * <pre>
     *   no blocked and no high-risk roads      -> FULLY_ACCESSIBLE
     *   high-risk roads but nothing blocked    -> PARTIALLY_ACCESSIBLE
     *   some blocked, but a way still open     -> RESTRICTED
     *   every road blocked                     -> CUT_OFF
     * </pre>
     */
    @Override
    @Transactional
    public AccessibilityResponse evaluateAccessibility(Long districtId) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));

        int open = (int) roadRepository.countByDistrictIdAndStatus(districtId, RoadStatus.OPEN);
        int partial = (int) roadRepository.countByDistrictIdAndStatus(
                districtId, RoadStatus.PARTIALLY_ACCESSIBLE);
        int highRisk = (int) roadRepository.countByDistrictIdAndStatus(
                districtId, RoadStatus.HIGH_RISK);
        int blocked = (int) roadRepository.countByDistrictIdAndStatus(
                districtId, RoadStatus.BLOCKED);
        int usable = open + partial + highRisk;

        AccessibilityLevel level;
        if (usable == 0 && blocked > 0) {
            level = AccessibilityLevel.CUT_OFF;
        } else if (blocked > 0) {
            level = AccessibilityLevel.RESTRICTED;
        } else if (highRisk > 0 || partial > 0) {
            level = AccessibilityLevel.PARTIALLY_ACCESSIBLE;
        } else {
            level = AccessibilityLevel.FULLY_ACCESSIBLE;
        }

        AccessibilityStatus status = new AccessibilityStatus();
        status.setDistrict(district);
        status.setAccessibilityLevel(level);
        status.setOpenRoads(open + partial);
        status.setBlockedRoads(blocked);
        status.setHighRiskRoads(highRisk);
        status.setRemarks("%d usable, %d high-risk, %d blocked road(s)"
                .formatted(usable, highRisk, blocked));
        status.setEvaluatedAt(OffsetDateTime.now());

        AccessibilityStatus saved = accessibilityRepository.save(status);
        log.debug("District {} evaluated as {}", district.getCode(), level);
        return districtMapper.toAccessibilityResponse(saved);
    }

    private void apply(District district, DistrictRequest request) {
        district.setCode(request.code());
        district.setName(request.name());
        district.setState(request.state());
        district.setCentroid(GeometryUtils.point(
                request.centroid().latitude(), request.centroid().longitude()));
        district.setPopulation(request.population());
        district.setAreaSqKm(request.areaSqKm());
    }

    private District requireByCode(String code) {
        return districtRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("District", "code", code));
    }
}
