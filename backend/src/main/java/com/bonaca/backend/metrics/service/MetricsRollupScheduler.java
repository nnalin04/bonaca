package com.bonaca.backend.metrics.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The nightly job behind the hybrid baseline/insight design in
 * docs/TECHNICAL/METRICS_IMPLEMENTATION_PLAN.md §3.2: recomputes every member's rolling baseline, then
 * generates that day's insights from the freshly recomputed baselines. No new infrastructure —
 * Spring's built-in scheduler, consistent with docs/TECHNICAL/BACKEND_CUSTOM_IMPLEMENTATION.md §5.
 */
@Component
public class MetricsRollupScheduler {

    private final BaselineService baselineService;
    private final InsightGenerationService insightGenerationService;

    public MetricsRollupScheduler(BaselineService baselineService, InsightGenerationService insightGenerationService) {
        this.baselineService = baselineService;
        this.insightGenerationService = insightGenerationService;
    }

    @Scheduled(cron = "${bonaca.metrics.rollup-cron}")
    public void runNightlyRollup() {
        baselineService.recomputeAllBaselines();
        insightGenerationService.generateDailyInsightsForAllMembers();
    }
}
