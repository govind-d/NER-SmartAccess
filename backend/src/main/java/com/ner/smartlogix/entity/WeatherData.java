package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.WeatherCondition;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One weather observation for one district.
 *
 * <p>Both a 24-hour and a 72-hour rainfall total are stored because they answer
 * different questions: a sudden cloudburst causes flash flooding, while three days of
 * steady rain saturates a hillside and causes landslides. The risk engine uses both.
 *
 * <p>{@code source} records where the row came from (MOCK, OPENWEATHER, IMD) so mock
 * demo data can never be mistaken for a real observation.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "weather_data")
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(name = "temperature_c")
    private Double temperatureC;

    @Column(name = "rainfall_mm_24h", nullable = false)
    private Double rainfallMm24h = 0.0;

    @Column(name = "rainfall_mm_72h", nullable = false)
    private Double rainfallMm72h = 0.0;

    private Double humidity;

    @Column(name = "wind_speed_kmph")
    private Double windSpeedKmph;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 20)
    private WeatherCondition condition;

    @Column(nullable = false, length = 30)
    private String source = "MOCK";

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
}
