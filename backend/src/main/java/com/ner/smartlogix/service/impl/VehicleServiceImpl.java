package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.GpsPingRequest;
import com.ner.smartlogix.dto.request.VehicleRequest;
import com.ner.smartlogix.dto.response.VehicleLocationResponse;
import com.ner.smartlogix.dto.response.VehicleResponse;
import com.ner.smartlogix.entity.*;
import com.ner.smartlogix.enums.*;
import com.ner.smartlogix.exception.BusinessRuleException;
import com.ner.smartlogix.exception.DuplicateResourceException;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.FleetMapper;
import com.ner.smartlogix.repository.*;
import com.ner.smartlogix.security.SecurityUtils;
import com.ner.smartlogix.service.AlertService;
import com.ner.smartlogix.service.VehicleService;
import com.ner.smartlogix.util.GeometryUtils;
import com.ner.smartlogix.websocket.RealtimeBroadcaster;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The fleet, and the GPS stream that comes off it.
 *
 * <p>{@link #recordPosition} is the hot path of the whole system - it runs once per
 * vehicle every few seconds - so it does exactly four things and nothing more:
 * store the ping, refresh the cached position on the vehicle, check whether the vehicle
 * has entered a dangerous stretch of road, and broadcast to the map.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    /** A vehicle within this distance of a road is considered to be on it. */
    private static final double GEOFENCE_RADIUS_METERS = 300;

    private final VehicleRepository vehicleRepository;
    private final VehicleLocationRepository locationRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final RoadRepository roadRepository;
    private final FleetMapper fleetMapper;
    private final RealtimeBroadcaster broadcaster;
    private final AlertService alertService;

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> findAll(Boolean active, VehicleType type) {
        List<Vehicle> vehicles;
        if (Boolean.TRUE.equals(active)) {
            vehicles = vehicleRepository.findByActiveTrue();
        } else if (type != null) {
            vehicles = vehicleRepository.findByVehicleType(type);
        } else {
            vehicles = vehicleRepository.findAll();
        }
        return vehicles.stream().map(fleetMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse findById(Long id) {
        return fleetMapper.toResponse(requireVehicle(id));
    }

    @Override
    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        if (vehicleRepository.findByVehicleNumber(request.vehicleNumber()).isPresent()) {
            throw new DuplicateResourceException("Vehicle", "number", request.vehicleNumber());
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber(request.vehicleNumber());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setCapacityTons(request.capacityTons());
        vehicle.setActive(true);
        if (request.driverId() != null) {
            vehicle.setDriver(requireDriver(request.driverId(), null));
        }
        return fleetMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle vehicle = requireVehicle(id);
        vehicle.setVehicleNumber(request.vehicleNumber());
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setCapacityTons(request.capacityTons());
        return fleetMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleResponse assignDriver(Long vehicleId, Long driverId) {
        Vehicle vehicle = requireVehicle(vehicleId);
        vehicle.setDriver(driverId == null ? null : requireDriver(driverId, vehicleId));
        return fleetMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = requireVehicle(id);
        if (!deliveryRepository.findByVehicleId(id).isEmpty()) {
            // Deleting would orphan delivery history, so refuse and suggest the safe move.
            throw new BusinessRuleException(
                    "This vehicle has deliveries. Deactivate it instead of deleting it.");
        }
        vehicleRepository.delete(vehicle);
    }

    @Override
    @Transactional
    public VehicleLocationResponse recordPosition(Long vehicleId, GpsPingRequest ping) {
        Vehicle vehicle = requireVehicle(vehicleId);
        assertCallerMayReportFor(vehicle);

        OffsetDateTime recordedAt = ping.recordedAt() == null
                ? OffsetDateTime.now() : ping.recordedAt();

        VehicleLocation location = new VehicleLocation();
        location.setVehicle(vehicle);
        location.setLatitude(ping.latitude());
        location.setLongitude(ping.longitude());
        location.setLocation(GeometryUtils.point(ping.latitude(), ping.longitude()));
        location.setSpeedKmph(ping.speedKmph());
        location.setHeading(ping.heading());
        location.setRecordedAt(recordedAt);
        VehicleLocation saved = locationRepository.save(location);

        // Cached copy so the live map needs one cheap query instead of a subquery
        // over hundreds of thousands of pings.
        vehicle.setLastLatitude(ping.latitude());
        vehicle.setLastLongitude(ping.longitude());
        vehicle.setLastSeenAt(recordedAt);
        vehicleRepository.save(vehicle);

        Delivery activeDelivery = activeDeliveryOf(vehicleId).orElse(null);
        VehicleLocationResponse response = fleetMapper.toResponse(saved, activeDelivery);

        checkDangerZone(vehicle, ping);
        broadcaster.vehicleMoved(vehicleId, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleLocationResponse latestPosition(Long vehicleId) {
        VehicleLocation location = locationRepository
                .findFirstByVehicleIdOrderByRecordedAtDesc(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No position recorded yet for vehicle " + vehicleId));
        return fleetMapper.toResponse(location, activeDeliveryOf(vehicleId).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleLocationResponse> history(Long vehicleId, OffsetDateTime from,
                                                 OffsetDateTime to) {
        OffsetDateTime start = from == null ? OffsetDateTime.now().minusHours(6) : from;
        OffsetDateTime end = to == null ? OffsetDateTime.now() : to;
        return locationRepository
                .findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(vehicleId, start, end)
                .stream()
                .map(location -> fleetMapper.toResponse(location, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleLocationResponse> liveFleet() {
        return vehicleRepository.findAllWithKnownPosition().stream()
                .map(vehicle -> new VehicleLocationResponse(
                        vehicle.getId(), vehicle.getVehicleNumber(),
                        vehicle.getLastLatitude(), vehicle.getLastLongitude(),
                        null, null,
                        activeDeliveryOf(vehicle.getId()).map(Delivery::getId).orElse(null),
                        activeDeliveryOf(vehicle.getId()).map(Delivery::getStatus).orElse(null),
                        vehicle.getLastSeenAt()))
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Geofencing. If the nearest road is BLOCKED or HIGH_RISK, warn immediately - this is
     * the feature that can actually stop a truck driving into a landslide zone.
     */
    private void checkDangerZone(Vehicle vehicle, GpsPingRequest ping) {
        roadRepository.findNearestRoad(ping.latitude(), ping.longitude(),
                        GEOFENCE_RADIUS_METERS)
                .filter(road -> road.getStatus() == RoadStatus.BLOCKED
                        || road.getStatus() == RoadStatus.HIGH_RISK)
                .ifPresent(road -> alertService.raiseDangerZoneAlert(vehicle, road));
    }

    private Optional<Delivery> activeDeliveryOf(Long vehicleId) {
        return deliveryRepository.findByVehicleId(vehicleId).stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.IN_TRANSIT
                        || delivery.getStatus() == DeliveryStatus.DELAYED)
                .findFirst();
    }

    /** A driver may push positions only for their own vehicle; an admin may do it for any. */
    private void assertCallerMayReportFor(Vehicle vehicle) {
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }
        Long callerId = SecurityUtils.currentUserId().orElse(null);
        boolean ownVehicle = vehicle.getDriver() != null && callerId != null
                && vehicle.getDriver().getId().equals(callerId);
        if (!ownVehicle) {
            throw new AccessDeniedException("You may only report positions for your own vehicle");
        }
    }

    private User requireDriver(Long driverId, Long allowedVehicleId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", driverId));
        boolean isDriver = driver.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.DRIVER);
        if (!isDriver) {
            throw new BusinessRuleException(
                    "User '" + driver.getUsername() + "' does not hold the DRIVER role");
        }
        vehicleRepository.findByDriverId(driverId)
                .filter(existing -> !existing.getId().equals(allowedVehicleId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("This driver is already assigned to vehicle "
                            + existing.getVehicleNumber());
                });
        return driver;
    }

    private Vehicle requireVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
    }
}
