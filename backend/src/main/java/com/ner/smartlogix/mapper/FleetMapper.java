package com.ner.smartlogix.mapper;

import com.ner.smartlogix.dto.response.DeliveryResponse;
import com.ner.smartlogix.dto.response.VehicleLocationResponse;
import com.ner.smartlogix.dto.response.VehicleResponse;
import com.ner.smartlogix.entity.Delivery;
import com.ner.smartlogix.entity.Vehicle;
import com.ner.smartlogix.entity.VehicleLocation;
import org.springframework.stereotype.Component;

/** Vehicles, positions and deliveries. */
@Component
public class FleetMapper {

    public VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                vehicle.getVehicleType(),
                vehicle.getCapacityTons(),
                vehicle.getDriver() == null ? null : vehicle.getDriver().getId(),
                vehicle.getDriver() == null ? null : vehicle.getDriver().getFullName(),
                vehicle.getDriver() == null ? null : vehicle.getDriver().getPhone(),
                vehicle.isActive(),
                vehicle.getLastLatitude(),
                vehicle.getLastLongitude(),
                vehicle.getLastSeenAt());
    }

    /** Builds the payload broadcast on /topic/vehicles. Delivery context may be absent. */
    public VehicleLocationResponse toResponse(VehicleLocation location, Delivery delivery) {
        Vehicle vehicle = location.getVehicle();
        return new VehicleLocationResponse(
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                location.getLatitude(),
                location.getLongitude(),
                location.getSpeedKmph(),
                location.getHeading(),
                delivery == null ? null : delivery.getId(),
                delivery == null ? null : delivery.getStatus(),
                location.getRecordedAt());
    }

    public DeliveryResponse toResponse(Delivery delivery) {
        Vehicle vehicle = delivery.getVehicle();
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getTrackingCode(),
                vehicle == null ? null : vehicle.getId(),
                vehicle == null ? null : vehicle.getVehicleNumber(),
                vehicle == null || vehicle.getDriver() == null
                        ? null : vehicle.getDriver().getFullName(),
                delivery.getRoute() == null ? null : delivery.getRoute().getId(),
                delivery.getSourceDistrict().getCode(),
                delivery.getSourceDistrict().getName(),
                delivery.getDestinationDistrict().getCode(),
                delivery.getDestinationDistrict().getName(),
                delivery.getSourceLabel(),
                delivery.getDestinationLabel(),
                delivery.getGoodsType(),
                delivery.getWeightTons(),
                delivery.getStatus(),
                delivery.getDispatchedAt(),
                delivery.getEta(),
                delivery.getDeliveredAt(),
                delivery.getDelayMinutes());
    }
}
