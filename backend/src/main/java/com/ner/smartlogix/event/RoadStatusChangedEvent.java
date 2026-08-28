package com.ner.smartlogix.event;

import com.ner.smartlogix.enums.RoadStatus;

/**
 * Published whenever a road changes status, by an official or by the incident listener.
 *
 * <p>Spring's application events are how this project keeps side effects out of the
 * service that caused them: RoadServiceImpl saves the road and publishes this record; a
 * listener raises the alert, another recomputes district accessibility, another warns
 * the affected deliveries. Adding a fifth consequence later means adding a listener, not
 * editing RoadServiceImpl.
 */
public record RoadStatusChangedEvent(Long roadId, String roadCode, String roadName,
                                     RoadStatus previousStatus, RoadStatus newStatus,
                                     Long districtId, String reason) {
}
