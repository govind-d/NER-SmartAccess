package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.response.WeatherResponse;
import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.entity.WeatherData;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.integration.weather.WeatherProvider;
import com.ner.smartlogix.repository.DistrictRepository;
import com.ner.smartlogix.repository.WeatherDataRepository;
import com.ner.smartlogix.service.AlertService;
import com.ner.smartlogix.service.WeatherService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps a current weather reading for every district.
 *
 * <p>Whichever {@link WeatherProvider} is configured gets injected - mock or real. This
 * service does not know or care which, which is the whole point of the interface.
 *
 * <p>The rainfall threshold below is the one place where "heavy rain" becomes an alert.
 * 64.5 mm in 24 hours is the India Meteorological Department's "heavy rainfall" category.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private static final double HEAVY_RAIN_MM_24H = 64.5;

    private final WeatherDataRepository weatherRepository;
    private final DistrictRepository districtRepository;
    private final WeatherProvider weatherProvider;
    private final AlertService alertService;

    @Override
    @Transactional(readOnly = true)
    public WeatherResponse current(String districtCode) {
        District district = requireDistrict(districtCode);
        return weatherRepository.findFirstByDistrictIdOrderByRecordedAtDesc(district.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No weather recorded yet for " + districtCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeatherResponse> history(String districtCode, int days) {
        District district = requireDistrict(districtCode);
        return weatherRepository.findByDistrictIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                        district.getId(), OffsetDateTime.now().minusDays(days),
                        OffsetDateTime.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeatherResponse> summary() {
        return districtRepository.findAll().stream()
                .map(district -> weatherRepository
                        .findFirstByDistrictIdOrderByRecordedAtDesc(district.getId())
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public int refreshAll() {
        int stored = 0;
        for (District district : districtRepository.findAll()) {
            try {
                var fetched = weatherProvider.fetch(district);
                if (fetched.isEmpty()) {
                    continue;
                }
                WeatherData weather = weatherRepository.save(fetched.get());
                stored++;

                if (weather.getRainfallMm24h() >= HEAVY_RAIN_MM_24H) {
                    alertService.raiseWeatherAlert(district, weather);
                }
            } catch (Exception ex) {
                // One district failing must not stop the sweep for the other twelve.
                log.warn("Weather refresh failed for {}: {}",
                        district.getCode(), ex.getMessage());
            }
        }
        log.info("Weather refreshed for {} district(s) via {}", stored,
                weatherProvider.providerName());
        return stored;
    }

    /**
     * Every 30 minutes. Together with the risk sweep five minutes later, this is what
     * makes the platform update itself: rain arrives, roads change colour, alerts appear.
     */
    @Scheduled(cron = "0 0,30 * * * *")
    public void scheduledRefresh() {
        try {
            refreshAll();
        } catch (Exception ex) {
            log.error("Scheduled weather sweep failed", ex);
        }
    }

    private WeatherResponse toResponse(WeatherData weather) {
        return new WeatherResponse(
                weather.getDistrict().getCode(),
                weather.getDistrict().getName(),
                weather.getTemperatureC(),
                weather.getRainfallMm24h(),
                weather.getRainfallMm72h(),
                weather.getHumidity(),
                weather.getWindSpeedKmph(),
                weather.getCondition(),
                weather.getSource(),
                weather.getRecordedAt());
    }

    private District requireDistrict(String code) {
        return districtRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("District", "code", code));
    }
}
