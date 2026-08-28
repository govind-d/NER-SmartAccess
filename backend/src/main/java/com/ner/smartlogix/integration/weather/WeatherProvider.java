package com.ner.smartlogix.integration.weather;

import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.entity.WeatherData;
import java.util.Optional;

/**
 * A source of weather observations.
 *
 * <p>Another swappable boundary. The mock provider generates NER-realistic monsoon data
 * so the whole system can be demonstrated with no API key and no internet; the
 * OpenWeather client talks to a real service. Switching between them is one line in
 * configuration, and no other class knows which one is running.
 */
public interface WeatherProvider {

    /** Empty rather than an exception when the source is unreachable: weather is optional. */
    Optional<WeatherData> fetch(District district);

    String providerName();
}
