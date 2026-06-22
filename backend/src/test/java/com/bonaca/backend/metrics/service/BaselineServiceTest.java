package com.bonaca.backend.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bonaca.backend.metrics.model.MetricBaseline;
import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.MemberMetricKey;
import com.bonaca.backend.metrics.repository.MetricBaselineRepository;
import com.bonaca.backend.metrics.repository.MetricReadingRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Contract from BaselineService's class Javadoc + docs/TECHNICAL/METRICS_IMPLEMENTATION_PLAN.md
 * §3.2/§9: deterministic mean/stddev over 14-21 valid (distinct calendar) days; fewer than 14
 * valid days leaves any existing baseline untouched rather than overwriting it.
 */
@ExtendWith(MockitoExtension.class)
class BaselineServiceTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Mock
    private MetricReadingRepository metricReadingRepository;

    @Mock
    private MetricBaselineRepository metricBaselineRepository;

    private BaselineService baselineService;

    @BeforeEach
    void setUp() {
        baselineService = new BaselineService(metricReadingRepository, metricBaselineRepository);
    }

    private static List<MetricReading> readingsOnDistinctDays(int days, double... values) {
        List<MetricReading> readings = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < days; i++) {
            double value = values[i % values.length];
            readings.add(new MetricReading(
                    MEMBER_ID, MetricType.HEART_RATE, value, "bpm", now.minus(i, ChronoUnit.DAYS), "device-1"));
        }
        return readings;
    }

    @Test
    void recomputeBaselineComputesMeanAndStddevOverValidDays() {
        List<MetricReading> readings = readingsOnDistinctDays(14, 70.0, 80.0);
        when(metricReadingRepository.findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(
                        org.mockito.ArgumentMatchers.eq(MEMBER_ID), org.mockito.ArgumentMatchers.eq(MetricType.HEART_RATE), any()))
                .thenReturn(readings);
        when(metricBaselineRepository.findByMemberIdAndMetricType(MEMBER_ID, MetricType.HEART_RATE)).thenReturn(Optional.empty());
        when(metricBaselineRepository.save(any(MetricBaseline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<MetricBaseline> result = baselineService.recomputeBaseline(MEMBER_ID, MetricType.HEART_RATE);

        assertThat(result).isPresent();
        // 7 readings of 70.0, 7 of 80.0 -> mean 75.0, population stddev 5.0
        assertThat(result.get().getBaselineMean()).isCloseTo(75.0, within(0.001));
        assertThat(result.get().getBaselineStddev()).isCloseTo(5.0, within(0.001));
        assertThat(result.get().getValidDayCount()).isEqualTo(14);
    }

    @Test
    void recomputeBaselineLeavesAnExistingBaselineUntouchedWhenFewerThan14ValidDaysExist() {
        List<MetricReading> readings = readingsOnDistinctDays(10, 70.0);
        when(metricReadingRepository.findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(
                        org.mockito.ArgumentMatchers.eq(MEMBER_ID), org.mockito.ArgumentMatchers.eq(MetricType.HEART_RATE), any()))
                .thenReturn(readings);
        MetricBaseline existing = new MetricBaseline(MEMBER_ID, MetricType.HEART_RATE, 60.0, 2.0, 14, Instant.now());
        when(metricBaselineRepository.findByMemberIdAndMetricType(MEMBER_ID, MetricType.HEART_RATE))
                .thenReturn(Optional.of(existing));

        Optional<MetricBaseline> result = baselineService.recomputeBaseline(MEMBER_ID, MetricType.HEART_RATE);

        assertThat(result).contains(existing);
        assertThat(result.get().getBaselineMean()).isEqualTo(60.0);
        verify(metricBaselineRepository, never()).save(any());
    }

    @Test
    void recomputeBaselineReturnsEmptyWhenThereIsNoExistingBaselineAndInsufficientData() {
        List<MetricReading> readings = readingsOnDistinctDays(5, 70.0);
        when(metricReadingRepository.findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(
                        org.mockito.ArgumentMatchers.eq(MEMBER_ID), org.mockito.ArgumentMatchers.eq(MetricType.HEART_RATE), any()))
                .thenReturn(readings);
        when(metricBaselineRepository.findByMemberIdAndMetricType(MEMBER_ID, MetricType.HEART_RATE)).thenReturn(Optional.empty());

        assertThat(baselineService.recomputeBaseline(MEMBER_ID, MetricType.HEART_RATE)).isEmpty();
    }

    @Test
    void recomputeBaselineUpdatesAnExistingBaselineInPlaceRatherThanCreatingANewRow() {
        List<MetricReading> readings = readingsOnDistinctDays(14, 90.0);
        when(metricReadingRepository.findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(
                        org.mockito.ArgumentMatchers.eq(MEMBER_ID), org.mockito.ArgumentMatchers.eq(MetricType.HEART_RATE), any()))
                .thenReturn(readings);
        MetricBaseline existing = new MetricBaseline(MEMBER_ID, MetricType.HEART_RATE, 60.0, 2.0, 14, Instant.now().minus(1, ChronoUnit.DAYS));
        when(metricBaselineRepository.findByMemberIdAndMetricType(MEMBER_ID, MetricType.HEART_RATE))
                .thenReturn(Optional.of(existing));
        when(metricBaselineRepository.save(any(MetricBaseline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<MetricBaseline> result = baselineService.recomputeBaseline(MEMBER_ID, MetricType.HEART_RATE);

        assertThat(result).containsSame(existing);
        assertThat(existing.getBaselineMean()).isCloseTo(90.0, within(0.001));
    }

    @Test
    void recomputeAllBaselinesRecomputesEveryDistinctMemberMetricPair() {
        UUID otherMember = UUID.randomUUID();
        when(metricReadingRepository.findDistinctMemberMetricPairsSince(any()))
                .thenReturn(List.of(
                        new MemberMetricKey(MEMBER_ID, MetricType.HEART_RATE),
                        new MemberMetricKey(otherMember, MetricType.STEPS)));
        when(metricReadingRepository.findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(metricBaselineRepository.findByMemberIdAndMetricType(any(), any())).thenReturn(Optional.empty());

        baselineService.recomputeAllBaselines();

        verify(metricReadingRepository).findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(
                org.mockito.ArgumentMatchers.eq(MEMBER_ID), org.mockito.ArgumentMatchers.eq(MetricType.HEART_RATE), any());
        verify(metricReadingRepository).findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(
                org.mockito.ArgumentMatchers.eq(otherMember), org.mockito.ArgumentMatchers.eq(MetricType.STEPS), any());
    }
}
