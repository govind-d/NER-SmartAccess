package com.ner.smartlogix.seed;

import com.ner.smartlogix.entity.*;
import com.ner.smartlogix.enums.*;
import com.ner.smartlogix.repository.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates demo users, vehicles, deliveries, incidents and weather rows on startup.
 *
 * <p>Why this is Java and not another Flyway migration: user passwords must be hashed
 * with BCrypt using a secret supplied at runtime. Putting a password - even a hashed
 * one - into a committed .sql file would defeat the point.
 *
 * <p>Three guards keep it out of production:
 * {@code @Profile("dev")}, the {@code app.seed.demo-data} flag, and an existence check
 * so that restarting the application never duplicates the data.
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DistrictRepository districtRepository;
    private final RoadRepository roadRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryRepository deliveryRepository;
    private final IncidentRepository incidentRepository;
    private final WeatherDataRepository weatherDataRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-password:}")
    private String demoPassword;

    /** SRID 4326 = latitude/longitude, matching every geometry column in the schema. */
    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public void run(String... args) {
        if (demoPassword == null || demoPassword.isBlank()) {
            log.warn("Demo seeding skipped: SEED_DEMO_PASSWORD is not set in backend/.env");
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Demo seeding skipped: users already exist");
            return;
        }

        log.info("Seeding demo data (dev profile)...");
        seedUsers();
        seedVehiclesAndDeliveries();
        seedIncidents();
        seedWeather();
        log.info("Demo seeding finished: {} users, {} vehicles, {} deliveries, {} incidents",
                userRepository.count(), vehicleRepository.count(),
                deliveryRepository.count(), incidentRepository.count());
    }

    private void seedUsers() {
        createUser("admin", "admin@ner.local", "System Administrator", RoleName.ADMIN, "AS-KAM");
        createUser("authority1", "authority1@ner.local", "B. Sangma",
                RoleName.AUTHORITY_OFFICIAL, "ML-EKH");
        createUser("manager1", "manager1@ner.local", "P. Deka",
                RoleName.LOGISTICS_MANAGER, "AS-KAM");
        createUser("officer1", "officer1@ner.local", "R. Marak",
                RoleName.FIELD_OFFICER, "ML-EKH");
        createUser("driver1", "driver1@ner.local", "T. Ao", RoleName.DRIVER, "NL-DIM");
        createUser("driver2", "driver2@ner.local", "L. Chakma", RoleName.DRIVER, "TR-WTR");
    }

    private User createUser(String username, String email, String fullName,
                            RoleName roleName, String districtCode) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        // The plain password never leaves this method; only the BCrypt hash is stored.
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.setEnabled(true);
        districtRepository.findByCode(districtCode).ifPresent(user::setDistrict);
        roleRepository.findByName(roleName).ifPresent(user::addRole);
        return userRepository.save(user);
    }

    private void seedVehiclesAndDeliveries() {
        List<User> drivers = userRepository.findEnabledByRole(RoleName.DRIVER);
        Optional<District> guwahati = districtRepository.findByCode("AS-KAM");
        Optional<District> shillong = districtRepository.findByCode("ML-EKH");
        Optional<District> kohima = districtRepository.findByCode("NL-KOH");
        if (guwahati.isEmpty() || shillong.isEmpty() || kohima.isEmpty()) {
            return;
        }

        Vehicle truck1 = createVehicle("AS01AB1234", VehicleType.TRUCK, 12.0,
                drivers.isEmpty() ? null : drivers.get(0), 26.1445, 91.7362);
        Vehicle van1 = createVehicle("TR01CD5678", VehicleType.RELIEF_VAN, 4.0,
                drivers.size() > 1 ? drivers.get(1) : null, 23.8315, 91.2868);
        createVehicle("ML05EF9012", VehicleType.MINI_TRUCK, 3.5, null, 25.5788, 91.8933);
        createVehicle("NL07GH3456", VehicleType.AMBULANCE, 1.0, null, 25.6751, 94.1086);

        createDelivery("NER-DEL-1001", truck1, guwahati.get(), shillong.get(),
                GoodsType.MEDICINE, 6.5, DeliveryStatus.IN_TRANSIT, 0);
        createDelivery("NER-DEL-1002", van1, guwahati.get(), kohima.get(),
                GoodsType.RELIEF_SUPPLIES, 3.2, DeliveryStatus.DELAYED, 145);
        createDelivery("NER-DEL-1003", null, shillong.get(), guwahati.get(),
                GoodsType.AGRI_PRODUCE, 8.0, DeliveryStatus.CREATED, 0);
        createDelivery("NER-DEL-1004", null, guwahati.get(), shillong.get(),
                GoodsType.FOOD, 10.0, DeliveryStatus.DELIVERED, 0);
    }

    private Vehicle createVehicle(String number, VehicleType type, Double capacity,
                                  User driver, double lat, double lon) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber(number);
        vehicle.setVehicleType(type);
        vehicle.setCapacityTons(capacity);
        vehicle.setDriver(driver);
        vehicle.setActive(true);
        vehicle.setLastLatitude(lat);
        vehicle.setLastLongitude(lon);
        vehicle.setLastSeenAt(OffsetDateTime.now());
        return vehicleRepository.save(vehicle);
    }

    private void createDelivery(String code, Vehicle vehicle, District source,
                                District destination, GoodsType goods, Double weight,
                                DeliveryStatus status, int delayMinutes) {
        Delivery delivery = new Delivery();
        delivery.setTrackingCode(code);
        delivery.setVehicle(vehicle);
        delivery.setSourceDistrict(source);
        delivery.setDestinationDistrict(destination);
        delivery.setSourceLabel(source.getName() + " Central Warehouse");
        delivery.setDestinationLabel(destination.getName() + " District Store");
        delivery.setGoodsType(goods);
        delivery.setWeightTons(weight);
        delivery.setStatus(status);
        delivery.setDelayMinutes(delayMinutes);
        if (status != DeliveryStatus.CREATED) {
            delivery.setDispatchedAt(OffsetDateTime.now().minusHours(5));
            delivery.setEta(OffsetDateTime.now().plusHours(2));
        }
        if (status == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(OffsetDateTime.now().minusHours(1));
        }
        deliveryRepository.save(delivery);
    }

    /**
     * A handful of historical incidents, weighted towards the monsoon months and
     * towards the landslide-prone corridors. This doubles as the first slice of
     * training data for the Phase 8 machine-learning model.
     */
    private void seedIncidents() {
        User reporter = userRepository.findByUsername("officer1").orElse(null);
        if (reporter == null) {
            return;
        }
        createIncident(reporter, IncidentType.LANDSLIDE, Severity.CRITICAL,
                "Hillside collapse blocking both lanes near km 14",
                25.2400, 93.0800, "SH-DHA-01", 3);
        createIncident(reporter, IncidentType.LANDSLIDE, Severity.HIGH,
                "Debris slide after 48 hours of continuous rain",
                27.2500, 88.5800, "NH10-RG-01", 12);
        createIncident(reporter, IncidentType.ROAD_DAMAGE, Severity.MEDIUM,
                "Surface washed out over a 200 metre stretch",
                25.8000, 93.9000, "NH02-DK-01", 25);
        createIncident(reporter, IncidentType.FLOOD, Severity.HIGH,
                "Water over the carriageway near the river crossing",
                24.9000, 92.6500, "NH06-SC-01", 40);
        createIncident(reporter, IncidentType.BRIDGE_DAMAGE, Severity.CRITICAL,
                "Approach slab of the river bridge collapsed",
                25.2400, 93.0800, "SH-DHA-01", 60);
        createIncident(reporter, IncidentType.TRAFFIC_CONGESTION, Severity.LOW,
                "Convoy movement causing slow traffic",
                25.4500, 92.2000, "NH06-SS-01", 8);
    }

    private void createIncident(User reporter, IncidentType type, Severity severity,
                                String description, double lat, double lon,
                                String roadCode, int daysAgo) {
        Incident incident = new Incident();
        incident.setClientUuid(UUID.randomUUID());
        incident.setIncidentType(type);
        incident.setSeverity(severity);
        incident.setDescription(description);
        incident.setLatitude(lat);
        incident.setLongitude(lon);
        incident.setLocation(point(lat, lon));
        incident.setReportedBy(reporter);
        incident.setStatus(IncidentStatus.VERIFIED);
        incident.setOccurredAt(OffsetDateTime.now().minusDays(daysAgo));
        roadRepository.findByCode(roadCode).ifPresent(road -> {
            incident.setRoad(road);
            incident.setDistrict(road.getDistrict());
        });
        incidentRepository.save(incident);
    }

    /**
     * One current observation per district. Hill districts get monsoon-scale rainfall
     * so that the risk engine has something interesting to react to on day one.
     */
    private void seedWeather() {
        for (District district : districtRepository.findAll()) {
            boolean hilly = List.of("ML-EKH", "AS-DHA", "SK-EAS", "NL-KOH", "MZ-AIZ")
                    .contains(district.getCode());
            WeatherData weather = new WeatherData();
            weather.setDistrict(district);
            weather.setTemperatureC(hilly ? 19.0 : 28.0);
            weather.setRainfallMm24h(hilly ? 128.0 : 22.0);
            weather.setRainfallMm72h(hilly ? 305.0 : 60.0);
            weather.setHumidity(hilly ? 94.0 : 78.0);
            weather.setWindSpeedKmph(hilly ? 24.0 : 12.0);
            weather.setCondition(hilly ? WeatherCondition.HEAVY_RAIN : WeatherCondition.CLOUDY);
            weather.setSource("MOCK");
            weather.setRecordedAt(OffsetDateTime.now());
            weatherDataRepository.save(weather);
        }
    }

    /** JTS wants (x, y) which is (longitude, latitude) - a classic source of bugs. */
    private Point point(double latitude, double longitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }
}
