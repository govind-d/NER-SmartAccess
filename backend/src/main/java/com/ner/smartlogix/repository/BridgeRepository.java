package com.ner.smartlogix.repository;

import com.ner.smartlogix.entity.Bridge;
import com.ner.smartlogix.enums.BridgeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BridgeRepository extends JpaRepository<Bridge, Long> {

    Optional<Bridge> findByCode(String code);

    List<Bridge> findByRoadId(Long roadId);

    List<Bridge> findByStatus(BridgeStatus status);

    /** Can this vehicle cross? Used by the routing filter in Phase 7. */
    List<Bridge> findByRoadIdAndLoadCapacityTonsLessThan(Long roadId, Double weightTons);
}
