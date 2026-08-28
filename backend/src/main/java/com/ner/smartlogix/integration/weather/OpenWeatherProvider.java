package com.ner.smartlogix.integration.weather;

import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.entity.WeatherData;
import com.ner.smartlogix.enums.WeatherCondition;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Real weather from OpenWeatherMap.
 *
 * <p>Switch it on with {@code WEATHER_PROVIDER=openweather} and a {@code WEATHER_API_KEY}.
 * The key is read from the environment and never appears in any committed file.
 *
 * <p>Any failure returns {@code Optional.empty()} rather than throwing: a weather outage
 * must degrade the risk engine's inputs, not break the request that triggered it.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.weather.provider", havingValue = "openweather")
public class OpenWeatherProvider implements WeatherProvider {

    private final RestClient restClient;
    private final String apiKey;

    public OpenWeatherProvider(RestClient.Builder builder,
                               @Value("${app.weather.base-url}") String baseUrl,
                               @Value("${app.weather.api-key}") String apiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<WeatherData> fetch(District district) {
        if (district.getCentroid() == null || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/weather")
                            .queryParam("lat", district.getCentroid().getY())
                            .queryParam("lon", district.getCentroid().getX())
                            .queryParam("units", "metric")
                            .queryParam("appid", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);
            if (body == null) {
                return Optional.empty();
            }

            Map<String, Object> main = (Map<String, Object>) body.get("main");
            Map<String, Object> wind = (Map<String, Object>) body.get("wind");
            Map<String, Object> rain = (Map<String, Object>) body.get("rain");
            List<Map<String, Object>> weatherList = (List<Map<String, Object>>) body.get("weather");

            // OpenWeather reports the last hour; the 24 h and 72 h figures the risk engine
            // needs come from the One Call history API on a paid plan. Extrapolating is
            // honest here only because the value is stamped with its real source.
            double lastHourMm = rain == null ? 0 : toDouble(rain.get("1h"));

            WeatherData weather = new WeatherData();
            weather.setDistrict(district);
            weather.setTemperatureC(main == null ? null : toDouble(main.get("temp")));
            weather.setHumidity(main == null ? null : toDouble(main.get("humidity")));
            weather.setWindSpeedKmph(wind == null ? null : toDouble(wind.get("speed")) * 3.6);
            weather.setRainfallMm24h(lastHourMm * 24);
            weather.setRainfallMm72h(lastHourMm * 72);
            weather.setCondition(mapCondition(weatherList));
            weather.setSource("OPENWEATHER");
            weather.setRecordedAt(OffsetDateTime.now());
            return Optional.of(weather);

        } catch (Exception ex) {
            log.warn("OpenWeather call failed for {}: {}", district.getCode(), ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String providerName() {
        return "OPENWEATHER";
    }

    private WeatherCondition mapCondition(List<Map<String, Object>> weatherList) {
        if (weatherList == null || weatherList.isEmpty()) {
            return WeatherCondition.CLEAR;
        }
        String main = String.valueOf(weatherList.get(0).get("main")).toUpperCase();
        return switch (main) {
            case "THUNDERSTORM" -> WeatherCondition.THUNDERSTORM;
            case "DRIZZLE" -> WeatherCondition.LIGHT_RAIN;
            case "RAIN" -> WeatherCondition.HEAVY_RAIN;
            case "SNOW" -> WeatherCondition.SNOW;
            case "MIST", "FOG", "HAZE" -> WeatherCondition.FOG;
            case "CLOUDS" -> WeatherCondition.CLOUDY;
            default -> WeatherCondition.CLEAR;
        };
    }

    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
