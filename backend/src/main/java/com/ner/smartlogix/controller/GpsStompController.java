package com.ner.smartlogix.controller;

import com.ner.smartlogix.dto.request.GpsPingRequest;
import com.ner.smartlogix.service.VehicleService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * The WebSocket counterpart of {@code POST /vehicles/{id}/locations}.
 *
 * <p>A driver's phone reporting every ten seconds over an already-open STOMP connection
 * avoids the cost of a fresh HTTP request each time - which on a weak 2G link in a valley
 * is the difference between a live track and a stuttering one.
 *
 * <p>The {@link Principal} comes from the JWT presented on the CONNECT frame, so the same
 * ownership rules as the REST endpoint apply inside the service.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GpsStompController {

    private final VehicleService vehicleService;

    /** Client sends to /app/gps/{vehicleId} */
    @MessageMapping("/gps/{vehicleId}")
    public void receivePosition(
            @org.springframework.messaging.handler.annotation.DestinationVariable Long vehicleId,
            @Payload GpsPingRequest ping,
            Principal principal) {
        try {
            vehicleService.recordPosition(vehicleId, ping);
        } catch (Exception ex) {
            // A WebSocket frame has nowhere to return an error to, so log and drop it.
            log.warn("Rejected STOMP position for vehicle {} from {}: {}",
                    vehicleId, principal == null ? "anonymous" : principal.getName(),
                    ex.getMessage());
        }
    }
}
