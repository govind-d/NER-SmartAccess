package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.GpsPingRequest;
import com.ner.smartlogix.dto.request.VehicleRequest;
import com.ner.smartlogix.dto.response.VehicleLocationResponse;
import com.ner.smartlogix.dto.response.VehicleResponse;
import com.ner.smartlogix.enums.VehicleType;
import java.time.OffsetDateTime;
import java.util.List;

public interface VehicleService {

    List<VehicleResponse> findAll(Boolean active, VehicleType type);

    VehicleResponse findById(Long id);

    VehicleResponse create(VehicleRequest request);

    VehicleResponse update(Long id, VehicleRequest request);

    VehicleResponse assignDriver(Long vehicleId, Long driverId);

    void delete(Long id);

    /** Stores a GPS ping, updates the cached position, checks geofences, broadcasts. */
    VehicleLocationResponse recordPosition(Long vehicleId, GpsPingRequest ping);

    VehicleLocationResponse latestPosition(Long vehicleId);

    List<VehicleLocationResponse> history(Long vehicleId, OffsetDateTime from,
                                          OffsetDateTime to);

    /** Last known position of every active vehicle, for the map's first paint. */
    List<VehicleLocationResponse> liveFleet();
}
