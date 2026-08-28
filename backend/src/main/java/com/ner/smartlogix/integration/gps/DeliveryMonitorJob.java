package com.ner.smartlogix.integration.gps;

import com.ner.smartlogix.entity.Delivery;
import com.ner.smartlogix.enums.DeliveryStatus;
import com.ner.smartlogix.event.DeliveryDelayedEvent;
import com.ner.smartlogix.repository.DeliveryRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notices consignments that have quietly missed their arrival time.
 *
 * <p>Without this job a delivery only becomes DELAYED when a person notices, which in
 * practice means when somebody complains. Checking every ten minutes turns the dashboard
 * counter into something trustworthy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryMonitorJob {

    /** Grace period before an overdue consignment is called delayed. */
    private static final int GRACE_MINUTES = 15;

    private final DeliveryRepository deliveryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void flagOverdueDeliveries() {
        try {
            List<Delivery> overdue = deliveryRepository.findOverdue(
                    OffsetDateTime.now().minusMinutes(GRACE_MINUTES));

            for (Delivery delivery : overdue) {
                int minutesLate = (int) Duration.between(delivery.getEta(),
                        OffsetDateTime.now()).toMinutes();

                delivery.setStatus(DeliveryStatus.DELAYED);
                delivery.setDelayMinutes(minutesLate);
                deliveryRepository.save(delivery);

                eventPublisher.publishEvent(new DeliveryDelayedEvent(delivery.getId(),
                        minutesLate,
                        "Estimated arrival passed %d minutes ago".formatted(minutesLate)));

                log.info("Delivery {} is {} minutes overdue",
                        delivery.getTrackingCode(), minutesLate);
            }
        } catch (Exception ex) {
            log.error("Overdue delivery check failed", ex);
        }
    }
}
