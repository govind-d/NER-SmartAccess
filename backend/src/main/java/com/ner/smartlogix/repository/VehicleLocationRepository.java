package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.VehicleLocation;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, Long> {

    /** Latest ping of one vehicle. Served by the (vehicle_id, recorded_at DESC) index. */
    Optional<VehicleLocation> findFirstByVehicleIdOrderByRecordedAtDesc(Long vehicleId);

    /** Trail drawn behind a vehicle on the map. */
    List<VehicleLocation> findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long vehicleId, OffsetDateTime from, OffsetDateTime to);

    /** Retention job - this table would otherwise grow without limit. */
    @Modifying
    @Query("DELETE FROM VehicleLocation l WHERE l.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
