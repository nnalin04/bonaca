package com.bonaca.backend.metrics.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsRollupSchedulerTest {

    @Mock
    private BaselineService baselineService;

    @Mock
    private InsightGenerationService insightGenerationService;

    @Test
    void nightlyRollupRecomputesBaselinesBeforeGeneratingInsights() {
        MetricsRollupScheduler scheduler = new MetricsRollupScheduler(baselineService, insightGenerationService);

        scheduler.runNightlyRollup();

        var inOrder = org.mockito.Mockito.inOrder(baselineService, insightGenerationService);
        inOrder.verify(baselineService).recomputeAllBaselines();
        inOrder.verify(insightGenerationService).generateDailyInsightsForAllMembers();
    }
}
