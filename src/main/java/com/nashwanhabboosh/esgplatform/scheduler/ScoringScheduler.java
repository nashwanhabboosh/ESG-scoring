package com.nashwanhabboosh.esgplatform.scheduler;

import com.nashwanhabboosh.esgplatform.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringScheduler {

    private final ScoringService scoringService;

    // Arbitrary midnight rerun
    @Scheduled(cron = "0 0 0 * * *")
    public void rescoreAllCompanies() {
        log.info("Scheduled rescore starting...");
        var scorecards = scoringService.scoreAllCompanies();
        log.info("Scheduled rescore complete. {} scorecards generated.", scorecards.size());
    }
}