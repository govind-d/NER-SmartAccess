package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Vehicle;
import com.ner.smartlogix.enums.VehicleType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    Optional<Vehicle> findByDriverId(Long driverId);

    List<Vehicle> findByActiveTrue();

    List<Vehicle> findByVehicleType(VehicleType vehicleType);

    long countByActiveTrue();

    /**
     * Every vehicle that has ever reported a position, for the live map's first paint.
     * Reads the cached last_* columns, so no join with vehicle_location is needed.
     */
    @Query("SELECT v FROM Vehicle v WHERE v.active = true AND v.lastLatitude IS NOT NULL")
    List<Vehicle> findAllWithKnownPosition();
}
