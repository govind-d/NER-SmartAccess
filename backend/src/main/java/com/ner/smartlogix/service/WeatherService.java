package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.response.WeatherResponse;
import java.util.List;

public interface WeatherService {

    WeatherResponse current(String districtCode);

    List<WeatherResponse> history(String districtCode, int days);

    List<WeatherResponse> summary();

    /** Pulls a fresh reading for every district and raises rainfall alerts. */
    int refreshAll();
}
