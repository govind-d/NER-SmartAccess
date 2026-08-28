package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.DeliveryRequest;
import com.ner.smartlogix.dto.response.DeliveryResponse;
import com.ner.smartlogix.enums.DeliveryStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryService {

    Page<DeliveryResponse> search(DeliveryStatus status, Long vehicleId, Pageable pageable);

    Page<DeliveryResponse> findMine(Pageable pageable);

    DeliveryResponse findById(Long id);

    DeliveryResponse findByTrackingCode(String trackingCode);

    DeliveryResponse create(DeliveryRequest request);

    /** Enforces the lifecycle CREATED to IN_TRANSIT to DELAYED/DELIVERED. */
    DeliveryResponse changeStatus(Long id, DeliveryStatus status);

    DeliveryResponse assignRoute(Long deliveryId, Long routeId);

    List<DeliveryResponse> findDelayed();

    void delete(Long id);
}
