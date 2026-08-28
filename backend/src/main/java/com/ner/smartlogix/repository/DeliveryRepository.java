package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Delivery;
import com.ner.smartlogix.enums.DeliveryStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByTrackingCode(String trackingCode);

    Page<Delivery> findByStatus(DeliveryStatus status, Pageable pageable);

    List<Delivery> findByVehicleId(Long vehicleId);

    /** A driver's own consignments - the DRIVER role sees nothing else. */
    Page<Delivery> findByVehicleDriverId(Long driverId, Pageable pageable);

    long countByStatus(DeliveryStatus status);

    /**
     * Which in-flight deliveries are affected when a road closes? Used by the incident
     * listener to flag them DELAYED and compute an alternate route.
     */
    @Query("""
           SELECT DISTINCT d FROM Delivery d
           JOIN d.route r JOIN r.segments s
           WHERE s.road.id = :roadId AND d.status IN
                 (com.ner.smartlogix.enums.DeliveryStatus.IN_TRANSIT,
                  com.ner.smartlogix.enums.DeliveryStatus.DELAYED)
           """)
    List<Delivery> findActiveDeliveriesUsingRoad(@Param("roadId") Long roadId);

    /** Deliveries whose ETA has already passed but which have not arrived. */
    @Query("""
           SELECT d FROM Delivery d
           WHERE d.status = com.ner.smartlogix.enums.DeliveryStatus.IN_TRANSIT
             AND d.eta < :now
           """)
    List<Delivery> findOverdue(@Param("now") OffsetDateTime now);

    @Query("SELECT d.status, COUNT(d) FROM Delivery d GROUP BY d.status")
    List<Object[]> countGroupedByStatus();
}
