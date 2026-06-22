package com.bonaca.backend.metrics.service;

import com.bonaca.backend.metrics.model.MetricBaseline;
import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.MemberMetricKey;
import com.bonaca.backend.metrics.repository.MetricBaselineRepository;
import com.bonaca.backend.metrics.repository.MetricReadingRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PRD §5: rolling baseline over 14-21 valid days, recalculated daily. "Valid day" here means a
 * calendar day (UTC) with at least one reading — the PRD's full definition (excluding days the
 * wearable wasn't worn or had sync gaps) needs a device-connection signal this pass doesn't have
 * (no Spike ingestion yet, see docs/TECHNICAL/METRICS_IMPLEMENTATION_PLAN.md §10); revisit this
 * once that signal exists.
 */
@Service
public class BaselineService {

    private static final int LOOKBACK_DAYS = 21;
    private static final int MIN_VALID_DAYS = 14;

    private final MetricReadingRepository metricReadingRepository;
    private final MetricBaselineRepository metricBaselineRepository;

    public BaselineService(MetricReadingRepository metricReadingRepository, MetricBaselineRepository metricBaselineRepository) {
        this.metricReadingRepository = metricReadingRepository;
        this.metricBaselineRepository = metricBaselineRepository;
    }

    /**
     * Recomputes and stores the baseline for one (member, metricType) pair. If fewer than
     * MIN_VALID_DAYS valid days exist in the lookback window, the existing baseline (if any) is
     * left untouched rather than being overwritten with a less-reliable computation.
     */
    @Transactional
    public Optional<MetricBaseline> recomputeBaseline(UUID memberId, MetricType metricType) {
        Instant since = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        List<MetricReading> readings = metricReadingRepository
                .findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(memberId, metricType, since);

        long validDays = readings.stream()
                .map(r -> r.getRecordedAt().atZone(ZoneOffset.UTC).toLocalDate())
                .distinct()
                .count();
        if (validDays < MIN_VALID_DAYS) {
            return metricBaselineRepository.findByMemberIdAndMetricType(memberId, metricType);
        }

        double mean = readings.stream().mapToDouble(MetricReading::getValue).average().orElse(0);
        double variance =
                readings.stream().mapToDouble(r -> Math.pow(r.getValue() - mean, 2)).average().orElse(0);
        double stddev = Math.sqrt(variance);
        Instant now = Instant.now();

        MetricBaseline baseline = metricBaselineRepository
                .findByMemberIdAndMetricType(memberId, metricType)
                .map(existing -> {
                    existing.update(mean, stddev, (int) validDays, now);
                    return existing;
                })
                .orElseGet(() -> new MetricBaseline(memberId, metricType, mean, stddev, (int) validDays, now));

        return Optional.of(metricBaselineRepository.save(baseline));
    }

    /** Called by MetricsRollupScheduler's nightly job — recomputes every (member, metricType) pair with recent data. */
    @Transactional
    public void recomputeAllBaselines() {
        Instant since = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        List<MemberMetricKey> pairs = metricReadingRepository.findDistinctMemberMetricPairsSince(since);
        pairs.forEach(pair -> recomputeBaseline(pair.memberId(), pair.metricType()));
    }
}
