package com.bonaca.backend.metrics.service;

import com.bonaca.backend.metrics.model.Insight;
import com.bonaca.backend.metrics.model.InsightKind;
import com.bonaca.backend.metrics.model.MetricBaseline;
import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.InsightRepository;
import com.bonaca.backend.metrics.repository.MetricBaselineRepository;
import com.bonaca.backend.metrics.repository.MetricReadingRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic, no-ML insight text generation from cached baselines — PRD §5. Two kinds of
 * insight are generated per member per day: a per-metric trend ("Heart rate has been higher than
 * usual today") when a metric's latest reading deviates meaningfully from its baseline, and one
 * composite Routine Consistency Score insight (metricType = null) combining screen time, outdoor
 * time, steps, and sleep — weighted so behaviour inputs (screen time + outdoor time = 0.6 of the
 * total weight) outweigh the single vitals input (sleep = 0.2), per PRD §5's "behaviour weighted
 * higher than vitals."
 */
@Service
public class InsightGenerationService {

    /** |z-score| below this isn't worth surfacing as a trend insight — "same as usual" is the default, unstated case. */
    private static final double TREND_THRESHOLD = 1.0;

    /** |z-score| at or above this is classified ANOMALY instead of TREND — see notifications plan doc §3.1. */
    private static final double ANOMALY_THRESHOLD = 2.0;

    private static final double STABLE_THRESHOLD = 0.5;
    private static final double SLIGHTLY_DIFFERENT_THRESHOLD = 1.5;

    private record WeightedInput(MetricType type, double weight) {
    }

    private static final List<WeightedInput> ROUTINE_CONSISTENCY_INPUTS = List.of(
            new WeightedInput(MetricType.SCREEN_TIME, 0.3),
            new WeightedInput(MetricType.OUTDOOR_TIME, 0.3),
            new WeightedInput(MetricType.STEPS, 0.2),
            new WeightedInput(MetricType.SLEEP, 0.2));

    private final MetricBaselineRepository metricBaselineRepository;
    private final MetricReadingRepository metricReadingRepository;
    private final InsightRepository insightRepository;

    public InsightGenerationService(
            MetricBaselineRepository metricBaselineRepository,
            MetricReadingRepository metricReadingRepository,
            InsightRepository insightRepository) {
        this.metricBaselineRepository = metricBaselineRepository;
        this.metricReadingRepository = metricReadingRepository;
        this.insightRepository = insightRepository;
    }

    /** Called by MetricsRollupScheduler's nightly job for every member with at least one baseline. */
    @Transactional
    public void generateDailyInsightsForAllMembers() {
        metricBaselineRepository.findAll().stream()
                .map(MetricBaseline::getMemberId)
                .distinct()
                .forEach(this::generateDailyInsights);
    }

    @Transactional
    public void generateDailyInsights(UUID memberId) {
        LocalDate today = LocalDate.now();
        List<MetricBaseline> baselines = metricBaselineRepository.findByMemberId(memberId);

        for (MetricBaseline baseline : baselines) {
            generateTrendInsight(memberId, baseline, today);
        }
        generateRoutineConsistencyInsight(memberId, baselines, today);
    }

    private void generateTrendInsight(UUID memberId, MetricBaseline baseline, LocalDate today) {
        Optional<Double> zScore = latestZScore(memberId, baseline);
        if (zScore.isEmpty() || Math.abs(zScore.get()) < TREND_THRESHOLD) {
            return;
        }
        String direction = zScore.get() > 0 ? "higher" : "lower";
        String text = displayName(baseline.getMetricType()) + " has been " + direction + " than usual today.";
        InsightKind kind = Math.abs(zScore.get()) >= ANOMALY_THRESHOLD ? InsightKind.ANOMALY : InsightKind.TREND;
        upsertMetricInsight(memberId, baseline.getMetricType(), text, kind, today);
    }

    private void generateRoutineConsistencyInsight(UUID memberId, List<MetricBaseline> baselines, LocalDate today) {
        Map<MetricType, MetricBaseline> byType =
                baselines.stream().collect(Collectors.toMap(MetricBaseline::getMetricType, b -> b));

        double weightedAbsZSum = 0;
        double weightUsed = 0;
        for (WeightedInput input : ROUTINE_CONSISTENCY_INPUTS) {
            MetricBaseline baseline = byType.get(input.type());
            if (baseline == null) {
                continue;
            }
            Optional<Double> zScore = latestZScore(memberId, baseline);
            if (zScore.isEmpty()) {
                continue;
            }
            weightedAbsZSum += Math.abs(zScore.get()) * input.weight();
            weightUsed += input.weight();
        }
        if (weightUsed == 0) {
            return;
        }

        double combined = weightedAbsZSum / weightUsed;
        String band = combined < STABLE_THRESHOLD
                ? "Stable"
                : combined < SLIGHTLY_DIFFERENT_THRESHOLD ? "Slightly different" : "Noticeably different";
        upsertCompositeInsight(memberId, "Routine Consistency: " + band + " from usual today.", today);
    }

    private Optional<Double> latestZScore(UUID memberId, MetricBaseline baseline) {
        if (baseline.getBaselineStddev() == 0) {
            return Optional.empty();
        }
        Optional<MetricReading> latest =
                metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(memberId, baseline.getMetricType());
        return latest.map(reading -> (reading.getValue() - baseline.getBaselineMean()) / baseline.getBaselineStddev());
    }

    private void upsertMetricInsight(UUID memberId, MetricType metricType, String text, InsightKind kind, LocalDate date) {
        Insight insight = insightRepository
                .findByMemberIdAndMetricTypeAndInsightDate(memberId, metricType, date)
                .map(existing -> {
                    existing.updateText(text);
                    existing.updateKind(kind);
                    return existing;
                })
                .orElseGet(() -> new Insight(memberId, metricType, text, kind, date));
        insightRepository.save(insight);
    }

    private void upsertCompositeInsight(UUID memberId, String text, LocalDate date) {
        Insight insight = insightRepository
                .findByMemberIdAndMetricTypeIsNullAndInsightDate(memberId, date)
                .map(existing -> {
                    existing.updateText(text);
                    return existing;
                })
                .orElseGet(() -> new Insight(memberId, null, text, InsightKind.TREND, date));
        insightRepository.save(insight);
    }

    private static String displayName(MetricType metricType) {
        String lower = metricType.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
