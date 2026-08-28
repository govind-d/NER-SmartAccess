package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.WeatherData;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    /** Newest observation for a district - the main input of the rainfall risk rule. */
    Optional<WeatherData> findFirstByDistrictIdOrderByRecordedAtDesc(Long districtId);

    List<WeatherData> findByDistrictIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long districtId, OffsetDateTime from, OffsetDateTime to);

    /** Districts currently receiving heavy rain - drives the rainfall alert sweep. */
    List<WeatherData> findByRainfallMm24hGreaterThanEqual(Double threshold);
}
