package com.ner.smartlogix.integration.weather;

import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.entity.WeatherData;
import com.ner.smartlogix.enums.WeatherCondition;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Generates plausible NER weather with no external call.
 *
 * <p>It is not random noise. The monsoon curve (June to September), the far higher
 * rainfall in the hill districts, and the three-day accumulation are all modelled, so the
 * risk engine is exercised with the kind of data it will really face. Meghalaya's hills
 * include some of the wettest inhabited places on earth, and the numbers here reflect that.
 *
 * <p>Every generated row is stored with {@code source = "MOCK"} so demonstration data can
 * never be mistaken for a real observation.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.weather.provider", havingValue = "mock",
        matchIfMissing = true)
public class MockWeatherProvider implements WeatherProvider {

    private static final List<String> HILL_DISTRICTS =
            List.of("ML-EKH", "ML-WGH", "AS-DHA", "SK-EAS", "NL-KOH", "MZ-AIZ", "AR-PAP");

    private final Random random = new Random();

    @Override
    public Optional<WeatherData> fetch(District district) {
        boolean hill = HILL_DISTRICTS.contains(district.getCode());
        Month month = OffsetDateTime.now().getMonth();
        boolean monsoon = month.getValue() >= 6 && month.getValue() <= 9;

        // Base daily rainfall in millimetres.
        double base;
        if (monsoon) {
            base = hill ? 60 + random.nextDouble() * 140 : 20 + random.nextDouble() * 60;
        } else {
            base = hill ? random.nextDouble() * 25 : random.nextDouble() * 10;
        }
        // Occasional cloudburst - the events that actually close roads.
        if (monsoon && random.nextDouble() < 0.12) {
            base += 80 + random.nextDouble() * 90;
        }

        double rainfall24h = round(base);
        double rainfall72h = round(rainfall24h * (1.8 + random.nextDouble() * 0.9));

        WeatherData weather = new WeatherData();
        weather.setDistrict(district);
        weather.setRainfallMm24h(rainfall24h);
        weather.setRainfallMm72h(rainfall72h);
        weather.setTemperatureC(round(hill ? 14 + random.nextDouble() * 12
                : 22 + random.nextDouble() * 12));
        weather.setHumidity(round(monsoon ? 80 + random.nextDouble() * 18
                : 55 + random.nextDouble() * 25));
        weather.setWindSpeedKmph(round(random.nextDouble() * 30));
        weather.setCondition(conditionFor(rainfall24h));
        weather.setSource("MOCK");
        weather.setRecordedAt(OffsetDateTime.now());
        return Optional.of(weather);
    }

    @Override
    public String providerName() {
        return "MOCK";
    }

    private WeatherCondition conditionFor(double rainfall24h) {
        if (rainfall24h >= 115.5) {
            return WeatherCondition.THUNDERSTORM;
        }
        if (rainfall24h >= 64.5) {
            return WeatherCondition.HEAVY_RAIN;
        }
        if (rainfall24h >= 15.6) {
            return WeatherCondition.LIGHT_RAIN;
        }
        if (rainfall24h > 2) {
            return WeatherCondition.CLOUDY;
        }
        return WeatherCondition.CLEAR;
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
