package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.DeliveryRequest;
import com.ner.smartlogix.dto.response.DeliveryResponse;
import com.ner.smartlogix.entity.Delivery;
import com.ner.smartlogix.entity.District;
import com.ner.smartlogix.entity.Route;
import com.ner.smartlogix.enums.DeliveryStatus;
import com.ner.smartlogix.event.DeliveryDelayedEvent;
import com.ner.smartlogix.exception.BusinessRuleException;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.FleetMapper;
import com.ner.smartlogix.repository.*;
import com.ner.smartlogix.security.SecurityUtils;
import com.ner.smartlogix.service.DeliveryService;
import com.ner.smartlogix.websocket.RealtimeBroadcaster;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consignments and their lifecycle.
 *
 * <p>The state machine below is the important part. Without it, a bug or a mistyped API
 * call could move a delivered consignment back to IN_TRANSIT and quietly corrupt every
 * delay statistic on the dashboard. Encoding the legal moves in one map makes the rule
 * readable and impossible to bypass.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    /** Which statuses may follow which. DELIVERED is terminal on purpose. */
    private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED_TRANSITIONS = Map.of(
            DeliveryStatus.CREATED, Set.of(DeliveryStatus.IN_TRANSIT),
            DeliveryStatus.IN_TRANSIT, Set.of(DeliveryStatus.DELAYED, DeliveryStatus.DELIVERED),
            DeliveryStatus.DELAYED, Set.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.DELIVERED),
            DeliveryStatus.DELIVERED, Set.of());

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final DeliveryRepository deliveryRepository;
    private final VehicleRepository vehicleRepository;
    private final DistrictRepository districtRepository;
    private final RouteRepository routeRepository;
    private final FleetMapper fleetMapper;
    private final RealtimeBroadcaster broadcaster;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryResponse> search(DeliveryStatus status, Long vehicleId,
                                         Pageable pageable) {
        if (status != null) {
            return deliveryRepository.findByStatus(status, pageable).map(fleetMapper::toResponse);
        }
        if (vehicleId != null) {
            List<DeliveryResponse> all = deliveryRepository.findByVehicleId(vehicleId).stream()
                    .map(fleetMapper::toResponse).toList();
            return new org.springframework.data.domain.PageImpl<>(all, pageable, all.size());
        }
        return deliveryRepository.findAll(pageable).map(fleetMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryResponse> findMine(Pageable pageable) {
        Long driverId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));
        return deliveryRepository.findByVehicleDriverId(driverId, pageable)
                .map(fleetMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResponse findById(Long id) {
        Delivery delivery = requireDelivery(id);
        assertMayView(delivery);
        return fleetMapper.toResponse(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResponse findByTrackingCode(String trackingCode) {
        return fleetMapper.toResponse(deliveryRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery", "trackingCode", trackingCode)));
    }

    @Override
    @Transactional
    public DeliveryResponse create(DeliveryRequest request) {
        District source = requireDistrict(request.sourceDistrictCode());
        District destination = requireDistrict(request.destinationDistrictCode());
        if (source.getId().equals(destination.getId())) {
            throw new BusinessRuleException("Source and destination districts must differ");
        }

        Delivery delivery = new Delivery();
        delivery.setTrackingCode(generateTrackingCode());
        delivery.setSourceDistrict(source);
        delivery.setDestinationDistrict(destination);
        delivery.setSourceLabel(request.sourceLabel());
        delivery.setDestinationLabel(request.destinationLabel());
        delivery.setGoodsType(request.goodsType());
        delivery.setWeightTons(request.weightTons());
        delivery.setStatus(DeliveryStatus.CREATED);

        if (request.vehicleId() != null) {
            vehicleRepository.findById(request.vehicleId()).ifPresentOrElse(vehicle -> {
                if (request.weightTons() != null && vehicle.getCapacityTons() != null
                        && request.weightTons() > vehicle.getCapacityTons()) {
                    throw new BusinessRuleException(
                            "Load of %.1f t exceeds the %.1f t capacity of %s".formatted(
                                    request.weightTons(), vehicle.getCapacityTons(),
                                    vehicle.getVehicleNumber()));
                }
                delivery.setVehicle(vehicle);
            }, () -> {
                throw new ResourceNotFoundException("Vehicle", "id", request.vehicleId());
            });
        }

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Delivery {} created: {} to {}", saved.getTrackingCode(),
                source.getCode(), destination.getCode());
        return fleetMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryResponse changeStatus(Long id, DeliveryStatus target) {
        Delivery delivery = requireDelivery(id);
        assertMayUpdate(delivery);

        DeliveryStatus current = delivery.getStatus();
        if (current == target) {
            return fleetMapper.toResponse(delivery);
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessRuleException(
                    "A delivery cannot go from %s to %s".formatted(current, target));
        }

        delivery.setStatus(target);
        switch (target) {
            case IN_TRANSIT -> {
                if (delivery.getDispatchedAt() == null) {
                    delivery.setDispatchedAt(OffsetDateTime.now());
                }
                if (delivery.getVehicle() == null) {
                    throw new BusinessRuleException(
                            "Assign a vehicle before dispatching this delivery");
                }
            }
            case DELIVERED -> delivery.setDeliveredAt(OffsetDateTime.now());
            case DELAYED -> {
                int delay = minutesLate(delivery);
                delivery.setDelayMinutes(delay);
                eventPublisher.publishEvent(new DeliveryDelayedEvent(
                        delivery.getId(), delay, "Status changed to DELAYED"));
            }
            default -> { }
        }

        Delivery saved = deliveryRepository.save(delivery);
        DeliveryResponse response = fleetMapper.toResponse(saved);
        broadcaster.deliveryUpdated(saved.getId(), response);
        log.info("Delivery {} moved {} -> {}", saved.getTrackingCode(), current, target);
        return response;
    }

    @Override
    @Transactional
    public DeliveryResponse assignRoute(Long deliveryId, Long routeId) {
        Delivery delivery = requireDelivery(deliveryId);
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));

        delivery.setRoute(route);
        // The ETA follows the route: assigning a slower, safer alternate immediately
        // shows up as a later arrival rather than as a surprise delay tomorrow.
        OffsetDateTime start = delivery.getDispatchedAt() == null
                ? OffsetDateTime.now() : delivery.getDispatchedAt();
        delivery.setEta(start.plusMinutes(route.getEstimatedDurationMin()));

        Delivery saved = deliveryRepository.save(delivery);
        DeliveryResponse response = fleetMapper.toResponse(saved);
        broadcaster.deliveryUpdated(saved.getId(), response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponse> findDelayed() {
        return deliveryRepository.findByStatus(DeliveryStatus.DELAYED, Pageable.unpaged())
                .map(fleetMapper::toResponse)
                .getContent();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Delivery delivery = requireDelivery(id);
        if (delivery.getStatus() != DeliveryStatus.CREATED) {
            throw new BusinessRuleException(
                    "Only a delivery that has not been dispatched can be deleted");
        }
        deliveryRepository.delete(delivery);
    }

    // ------------------------------------------------------------------ helpers

    private int minutesLate(Delivery delivery) {
        if (delivery.getEta() == null) {
            return delivery.getDelayMinutes();
        }
        long minutes = java.time.Duration.between(delivery.getEta(), OffsetDateTime.now())
                .toMinutes();
        return (int) Math.max(0, minutes);
    }

    /** Human-friendly, unambiguous code: no O/0 or I/1 to confuse anyone reading it aloud. */
    private String generateTrackingCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder builder = new StringBuilder("NER-");
            for (int i = 0; i < 8; i++) {
                builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            String code = builder.toString();
            if (deliveryRepository.findByTrackingCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique tracking code");
    }

    private void assertMayView(Delivery delivery) {
        if (SecurityUtils.hasRole("DRIVER") && !SecurityUtils.hasRole("ADMIN")
                && !SecurityUtils.hasRole("LOGISTICS_MANAGER")
                && !SecurityUtils.hasRole("AUTHORITY_OFFICIAL")) {
            Long callerId = SecurityUtils.currentUserId().orElse(null);
            boolean own = delivery.getVehicle() != null
                    && delivery.getVehicle().getDriver() != null
                    && delivery.getVehicle().getDriver().getId().equals(callerId);
            if (!own) {
                throw new AccessDeniedException("You may only view your own deliveries");
            }
        }
    }

    private void assertMayUpdate(Delivery delivery) {
        assertMayView(delivery);
    }

    private Delivery requireDelivery(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", id));
    }

    private District requireDistrict(String code) {
        return districtRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("District", "code", code));
    }
}
