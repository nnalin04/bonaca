package com.bonaca.backend.metrics.service;

import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.metrics.dto.InsightResponse;
import com.bonaca.backend.metrics.dto.MetricDetailResponse;
import com.bonaca.backend.metrics.dto.MetricRange;
import com.bonaca.backend.metrics.dto.MetricSummaryResponse;
import com.bonaca.backend.metrics.model.MetricBaseline;
import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricTrendLabel;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.InsightRepository;
import com.bonaca.backend.metrics.repository.MetricBaselineRepository;
import com.bonaca.backend.metrics.repository.MetricReadingRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * The live read path — see docs/TECHNICAL/METRICS_IMPLEMENTATION_PLAN.md §3.2: range/average/
 * chart data is always computed fresh from raw readings; only the trend-label comparison uses
 * the nightly-cached MetricBaseline. Authorization is scope-level (canViewScope), not just
 * member-level — PRD §11.2's Vitals/Activity/Behaviour categories aren't interchangeable.
 */
@Service
public class MetricsQueryService {

    /** Same threshold InsightGenerationService uses for "notable" — kept in sync deliberately, not shared via a constant, since the two callers' tests should each pin their own behavior. */
    private static final double TREND_THRESHOLD = 1.0;

    private final MemberRepository memberRepository;
    private final MemberPermissions permissions;
    private final MetricReadingRepository metricReadingRepository;
    private final MetricBaselineRepository metricBaselineRepository;
    private final InsightRepository insightRepository;

    public MetricsQueryService(
            MemberRepository memberRepository,
            MemberPermissions permissions,
            MetricReadingRepository metricReadingRepository,
            MetricBaselineRepository metricBaselineRepository,
            InsightRepository insightRepository) {
        this.memberRepository = memberRepository;
        this.permissions = permissions;
        this.metricReadingRepository = metricReadingRepository;
        this.metricBaselineRepository = metricBaselineRepository;
        this.insightRepository = insightRepository;
    }

    /**
     * Metric types the requester lacks scope access to are silently omitted (PRD §12: a reduced
     * grant means "some data is no longer shared," not an error) — but if the requester can't
     * view the member at all, that's a real 403.
     */
    public List<MetricSummaryResponse> getMemberMetricsSummary(UUID requesterUserId, UUID memberId, MetricRange range) {
        Member requester = permissions.requireMemberForUser(requesterUserId);
        Member target = requireMember(memberId);
        if (!permissions.canView(requester, target)) {
            throw new ForbiddenMemberAccessException("You don't have access to this member");
        }

        Instant since = range.since(Instant.now());
        List<MetricReading> readings = metricReadingRepository.findByMemberIdAndRecordedAtAfterOrderByRecordedAtAsc(memberId, since);
        Map<MetricType, List<MetricReading>> byType = readings.stream().collect(Collectors.groupingBy(MetricReading::getMetricType));

        return byType.entrySet().stream()
                .filter(entry -> permissions.canViewScope(requester, target, entry.getKey().scope()))
                .map(entry -> toSummary(memberId, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(MetricSummaryResponse::deviationScore).reversed())
                .toList();
    }

    /** A directed request for one specific metric type the requester lacks scope access to is a real 403, not a silent omission. */
    public MetricDetailResponse getMetricDetail(UUID requesterUserId, UUID memberId, MetricType metricType, MetricRange range) {
        Member requester = permissions.requireMemberForUser(requesterUserId);
        Member target = requireMember(memberId);
        if (!permissions.canViewScope(requester, target, metricType.scope())) {
            throw new ForbiddenMemberAccessException("You don't have access to this metric");
        }

        Instant since = range.since(Instant.now());
        List<MetricReading> readings = metricReadingRepository
                .findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(memberId, metricType, since);
        if (readings.isEmpty()) {
            return new MetricDetailResponse(metricType.name().toLowerCase(), false, null, null, null, null, List.of(), null, null);
        }

        double average = average(readings);
        double min = readings.stream().mapToDouble(MetricReading::getValue).min().orElse(average);
        double max = readings.stream().mapToDouble(MetricReading::getValue).max().orElse(average);
        Optional<MetricBaseline> baseline = metricBaselineRepository.findByMemberIdAndMetricType(memberId, metricType);
        String insightText = insightRepository
                .findByMemberIdAndMetricTypeAndInsightDate(memberId, metricType, LocalDate.now())
                .map(insight -> insight.getGeneratedText())
                .orElse(null);

        return new MetricDetailResponse(
                metricType.name().toLowerCase(),
                true,
                average,
                readings.get(0).getUnit(),
                min,
                max,
                readings.stream().map(MetricReading::getValue).toList(),
                trendLabel(average, baseline).map(label -> label.name().toLowerCase()).orElse(null),
                insightText);
    }

    public List<InsightResponse> listInsights(UUID requesterUserId, UUID memberId) {
        Member requester = permissions.requireMemberForUser(requesterUserId);
        Member target = requireMember(memberId);
        if (!permissions.canView(requester, target)) {
            throw new ForbiddenMemberAccessException("You don't have access to this member");
        }

        return insightRepository.findByMemberIdOrderByInsightDateDesc(memberId).stream()
                .filter(insight -> insight.getMetricType() == null
                        || permissions.canViewScope(requester, target, insight.getMetricType().scope()))
                .map(InsightResponse::from)
                .toList();
    }

    private Member requireMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new MemberNotFoundException("Member not found"));
    }

    private MetricSummaryResponse toSummary(UUID memberId, MetricType metricType, List<MetricReading> readings) {
        double average = average(readings);
        double min = readings.stream().mapToDouble(MetricReading::getValue).min().orElse(average);
        double max = readings.stream().mapToDouble(MetricReading::getValue).max().orElse(average);
        Optional<MetricBaseline> baseline = metricBaselineRepository.findByMemberIdAndMetricType(memberId, metricType);

        return new MetricSummaryResponse(
                metricType.name().toLowerCase(),
                average,
                readings.get(0).getUnit(),
                min,
                max,
                trendLabel(average, baseline).map(label -> label.name().toLowerCase()).orElse(null),
                deviationScore(average, baseline));
    }

    private static double average(List<MetricReading> readings) {
        return readings.stream().mapToDouble(MetricReading::getValue).average().orElse(0);
    }

    private static double deviationScore(double average, Optional<MetricBaseline> baseline) {
        if (baseline.isEmpty() || baseline.get().getBaselineStddev() == 0) {
            return 0;
        }
        return Math.abs(average - baseline.get().getBaselineMean()) / baseline.get().getBaselineStddev();
    }

    private static Optional<MetricTrendLabel> trendLabel(double average, Optional<MetricBaseline> baseline) {
        if (baseline.isEmpty() || baseline.get().getBaselineStddev() == 0) {
            return Optional.empty();
        }
        double z = (average - baseline.get().getBaselineMean()) / baseline.get().getBaselineStddev();
        if (z > TREND_THRESHOLD) {
            return Optional.of(MetricTrendLabel.HIGHER_THAN_USUAL);
        }
        if (z < -TREND_THRESHOLD) {
            return Optional.of(MetricTrendLabel.LOWER_THAN_USUAL);
        }
        return Optional.of(MetricTrendLabel.SAME_AS_USUAL);
    }
}
