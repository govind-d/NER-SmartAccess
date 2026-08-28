package com.ner.smartlogix.mapper;

import com.ner.smartlogix.dto.response.AccessibilityResponse;
import com.ner.smartlogix.dto.response.DistrictResponse;
import com.ner.smartlogix.entity.AccessibilityStatus;
import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.enums.AccessibilityLevel;
import org.springframework.stereotype.Component;

/**
 * Hand-written mapper.
 *
 * <p>MapStruct was used for {@code UserMapper} because that conversion is a plain field
 * copy. The domain mappers are written by hand instead, because they compose data from
 * more than one source (a district plus its newest accessibility snapshot) and unpack
 * JTS geometry into latitude/longitude - work MapStruct would need so many
 * {@code @Mapping(expression = ...)} escapes for that the generated code would be harder
 * to follow than the plain Java below.
 */
@Component
public class DistrictMapper {

    public DistrictResponse toResponse(District district, AccessibilityStatus latest) {
        return new DistrictResponse(
                district.getId(),
                district.getCode(),
                district.getName(),
                district.getState(),
                district.getCentroid() == null ? null : district.getCentroid().getY(), // y = lat
                district.getCentroid() == null ? null : district.getCentroid().getX(), // x = lon
                district.getPopulation(),
                district.getAreaSqKm(),
                latest == null ? AccessibilityLevel.FULLY_ACCESSIBLE : latest.getAccessibilityLevel(),
                latest == null ? 0 : latest.getOpenRoads(),
                latest == null ? 0 : latest.getBlockedRoads(),
                latest == null ? 0 : latest.getHighRiskRoads());
    }

    public AccessibilityResponse toAccessibilityResponse(AccessibilityStatus status) {
        return new AccessibilityResponse(
                status.getDistrict().getCode(),
                status.getDistrict().getName(),
                status.getAccessibilityLevel(),
                status.getOpenRoads(),
                status.getBlockedRoads(),
                status.getHighRiskRoads(),
                status.getRemarks(),
                status.getEvaluatedAt());
    }
}
