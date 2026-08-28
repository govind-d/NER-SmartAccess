package com.ner.smartlogix.integration.gps;

import com.ner.smartlogix.entity.Delivery;
import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.entity.Vehicle;
import com.ner.smartlogix.entity.VehicleLocation;
import com.ner.smartlogix.enums.DeliveryStatus;
import com.ner.smartlogix.mapper.FleetMapper;
import com.ner.smartlogix.repository.DeliveryRepository;
import com.ner.smartlogix.repository.VehicleLocationRepository;
import com.ner.smartlogix.repository.VehicleRepository;
import com.ner.smartlogix.util.GeometryUtils;
import com.ner.smartlogix.websocket.RealtimeBroadcaster;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves vehicles across the map so the platform can be demonstrated without a fleet of
 * real trucks.
 *
 * <p>Every tick, each vehicle carrying an in-transit consignment is advanced a short way
 * along the line from its source district towards its destination, with a little random
 * jitter so the track looks like driving rather than a ruler. When it arrives, the
 * delivery is marked DELIVERED - so a demonstration left running produces a plausible
 * day of operations on its own.
 *
 * <p>Two safeguards keep it out of production: it is a {@code dev}-profile bean, and it
 * only runs when {@code GPS_SIMULATOR_ENABLED=true}.
 *
 * <p>It writes through the repositories rather than through {@code VehicleService},
 * because that service quite rightly refuses position reports from anyone who is not the
 * assigned driver, and a background job has no logged-in user. Simulated data is honest
 * about being simulated instead of borrowing somebody's credentials.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.gps-simulator.enabled", havingValue = "true")
@RequiredArgsConstructor
public class GpsSimulator {

    /** Fraction of the remaining journey covered per tick. */
    private static final double STEP_FRACTION = 0.06;
    /** Below this distance in kilometres the vehicle counts as arrived. */
    private static final double ARRIVAL_KM = 2.0;

    private final VehicleRepository vehicleRepository;
    private final VehicleLocationRepository locationRepository;
    private final DeliveryRepository deliveryRepository;
    private final FleetMapper fleetMapper;
    private final RealtimeBroadcaster broadcaster;
    private final Random random = new Random();

    @Scheduled(fixedDelayString = "${app.gps-simulator.interval-ms:10000}")
    @Transactional
    public void tick() {
        try {
            List<Delivery> moving = deliveryRepository
                    .findByStatus(DeliveryStatus.IN_TRANSIT,
                            org.springframework.data.domain.Pageable.unpaged())
                    .getContent();
            for (Delivery delivery : moving) {
                if (delivery.getVehicle() != null) {
                    advance(delivery);
                }
            }
        } catch (Exception ex) {
            // A scheduled method that throws is never scheduled again, so nothing escapes.
            log.error("GPS simulator tick failed", ex);
        }
    }

    private void advance(Delivery delivery) {
        Vehicle vehicle = delivery.getVehicle();
        District source = delivery.getSourceDistrict();
        District destination = delivery.getDestinationDistrict();
        if (source.getCentroid() == null || destination.getCentroid() == null) {
            return;
        }

        double targetLat = destination.getCentroid().getY();
        double targetLon = destination.getCentroid().getX();

        // A vehicle that has never reported starts at its source district.
        double currentLat = vehicle.getLastLatitude() == null
                ? source.getCentroid().getY() : vehicle.getLastLatitude();
        double currentLon = vehicle.getLastLongitude() == null
                ? source.getCentroid().getX() : vehicle.getLastLongitude();

        double remainingKm = GeometryUtils.haversineKm(currentLat, currentLon,
                targetLat, targetLon);
        if (remainingKm <= ARRIVAL_KM) {
            arrive(delivery, vehicle, targetLat, targetLon);
            return;
        }

        // Step towards the destination, with a small sideways wobble so the trace looks
        // like a road rather than a straight line.
        double jitter = (random.nextDouble() - 0.5) * 0.004;
        double nextLat = currentLat + (targetLat - currentLat) * STEP_FRACTION + jitter;
        double nextLon = currentLon + (targetLon - currentLon) * STEP_FRACTION + jitter;

        double stepKm = GeometryUtils.haversineKm(currentLat, currentLon, nextLat, nextLon);
        double speedKmph = Math.round(stepKm / (10.0 / 3600.0));   // 10-second ticks
        double heading = bearing(currentLat, currentLon, nextLat, nextLon);

        VehicleLocation location = new VehicleLocation();
        location.setVehicle(vehicle);
        location.setLatitude(round(nextLat));
        location.setLongitude(round(nextLon));
        location.setLocation(GeometryUtils.point(nextLat, nextLon));
        location.setSpeedKmph(Math.min(80, speedKmph));
        location.setHeading(heading);
        location.setRecordedAt(OffsetDateTime.now());
        VehicleLocation saved = locationRepository.save(location);

        vehicle.setLastLatitude(location.getLatitude());
        vehicle.setLastLongitude(location.getLongitude());
        vehicle.setLastSeenAt(location.getRecordedAt());
        vehicleRepository.save(vehicle);

        broadcaster.vehicleMoved(vehicle.getId(), fleetMapper.toResponse(saved, delivery));
    }

    private void arrive(Delivery delivery, Vehicle vehicle, double lat, double lon) {
        vehicle.setLastLatitude(round(lat));
        vehicle.setLastLongitude(round(lon));
        vehicle.setLastSeenAt(OffsetDateTime.now());
        vehicleRepository.save(vehicle);

        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(OffsetDateTime.now());
        deliveryRepository.save(delivery);

        log.info("[SIMULATOR] Delivery {} arrived at {}", delivery.getTrackingCode(),
                delivery.getDestinationDistrict().getName());
        broadcaster.deliveryUpdated(delivery.getId(), fleetMapper.toResponse(delivery));
    }

    /** Compass bearing in degrees, used to rotate the marker on the map. */
    private double bearing(double fromLat, double fromLon, double toLat, double toLon) {
        double dLon = Math.toRadians(toLon - fromLon);
        double lat1 = Math.toRadians(fromLat);
        double lat2 = Math.toRadians(toLat);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private double round(double value) {
        return Math.round(value * 1_000_000) / 1_000_000.0;
    }
}
