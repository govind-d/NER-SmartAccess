package com.ner.smartlogix.ml;

import com.ner.smartlogix.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the risk picture current without anyone pressing a button.
 *
 * <p>This is what makes the platform feel alive during a demonstration: rainfall arrives
 * from the weather sweep, this job re-scores the affected roads half an hour later, roads
 * change colour on the map and alerts appear on their own.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskAssessmentJob {

    private final RiskService riskService;

    /** Every 30 minutes, offset from the weather sweep so it runs on fresh readings. */
    @Scheduled(cron = "0 5,35 * * * *")
    public void reassessNetwork() {
        try {
            int changed = riskService.reassessAll(null);
            if (changed > 0) {
                log.info("Scheduled risk sweep changed the level of {} road(s)", changed);
            }
        } catch (Exception ex) {
            // A scheduled job must never die: if it throws, Spring stops rescheduling it.
            log.error("Scheduled risk sweep failed", ex);
        }
    }
}
