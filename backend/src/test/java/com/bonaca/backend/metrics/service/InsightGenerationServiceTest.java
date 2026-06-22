package com.bonaca.backend.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bonaca.backend.metrics.model.Insight;
import com.bonaca.backend.metrics.model.MetricBaseline;
import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.InsightRepository;
import com.bonaca.backend.metrics.repository.MetricBaselineRepository;
import com.bonaca.backend.metrics.repository.MetricReadingRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Contract from InsightGenerationService's class Javadoc: a per-metric trend insight is
 * generated only when the latest reading deviates meaningfully (|z| >= 1.0) from the cached
 * baseline; the composite Routine Consistency insight combines screen time/outdoor time/steps/
 * sleep with behaviour (screen time + outdoor time = 0.6) outweighing vitals (sleep = 0.2).
 */
@ExtendWith(MockitoExtension.class)
class InsightGenerationServiceTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Mock
    private MetricBaselineRepository metricBaselineRepository;

    @Mock
    private MetricReadingRepository metricReadingRepository;

    @Mock
    private InsightRepository insightRepository;

    private InsightGenerationService service;

    @BeforeEach
    void setUp() {
        service = new InsightGenerationService(metricBaselineRepository, metricReadingRepository, insightRepository);
    }

    private static MetricBaseline baseline(MetricType type, double mean, double stddev) {
        return new MetricBaseline(MEMBER_ID, type, mean, stddev, 14, Instant.now());
    }

    private static MetricReading reading(MetricType type, double value) {
        return new MetricReading(MEMBER_ID, type, value, "unit", Instant.now(), "device-1");
    }

    @Test
    void generatesATrendInsightWhenTheLatestReadingDeviatesNotablyButBelowTheAnomalyThreshold() {
        MetricBaseline baseline = baseline(MetricType.HEART_RATE, 70.0, 5.0);
        when(metricBaselineRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(baseline));
        when(metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(MEMBER_ID, MetricType.HEART_RATE))
                .thenReturn(Optional.of(reading(MetricType.HEART_RATE, 77.5))); // z = 1.5
        when(insightRepository.findByMemberIdAndMetricTypeAndInsightDate(eq(MEMBER_ID), eq(MetricType.HEART_RATE), any()))
                .thenReturn(Optional.empty());

        service.generateDailyInsights(MEMBER_ID);

        ArgumentCaptor<Insight> captor = ArgumentCaptor.forClass(Insight.class);
        verify(insightRepository).save(captor.capture());
        assertThat(captor.getValue().getGeneratedText()).contains("Heart rate").contains("higher");
        assertThat(captor.getValue().getMetricType()).isEqualTo(MetricType.HEART_RATE);
        assertThat(captor.getValue().getKind()).isEqualTo(com.bonaca.backend.metrics.model.InsightKind.TREND);
    }

    @Test
    void generatesAnAnomalyInsightWhenTheLatestReadingDeviatesStrongly() {
        MetricBaseline baseline = baseline(MetricType.HEART_RATE, 70.0, 5.0);
        when(metricBaselineRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(baseline));
        when(metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(MEMBER_ID, MetricType.HEART_RATE))
                .thenReturn(Optional.of(reading(MetricType.HEART_RATE, 90.0))); // z = 4.0
        when(insightRepository.findByMemberIdAndMetricTypeAndInsightDate(eq(MEMBER_ID), eq(MetricType.HEART_RATE), any()))
                .thenReturn(Optional.empty());

        service.generateDailyInsights(MEMBER_ID);

        ArgumentCaptor<Insight> captor = ArgumentCaptor.forClass(Insight.class);
        verify(insightRepository).save(captor.capture());
        assertThat(captor.getValue().getKind()).isEqualTo(com.bonaca.backend.metrics.model.InsightKind.ANOMALY);
    }

    @Test
    void doesNotGenerateATrendInsightWhenTheLatestReadingIsCloseToBaseline() {
        MetricBaseline baseline = baseline(MetricType.HEART_RATE, 70.0, 5.0);
        when(metricBaselineRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(baseline));
        when(metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(MEMBER_ID, MetricType.HEART_RATE))
                .thenReturn(Optional.of(reading(MetricType.HEART_RATE, 71.0))); // z = 0.2

        service.generateDailyInsights(MEMBER_ID);

        verify(insightRepository, never()).save(any());
    }

    @Test
    void reRunningOnTheSameDayUpdatesTheExistingInsightInsteadOfDuplicating() {
        MetricBaseline baseline = baseline(MetricType.HEART_RATE, 70.0, 5.0);
        when(metricBaselineRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(baseline));
        when(metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(MEMBER_ID, MetricType.HEART_RATE))
                .thenReturn(Optional.of(reading(MetricType.HEART_RATE, 50.0))); // z = -4.0, "lower"
        Insight existing = new Insight(MEMBER_ID, MetricType.HEART_RATE, "stale text", com.bonaca.backend.metrics.model.InsightKind.TREND, LocalDate.now());
        when(insightRepository.findByMemberIdAndMetricTypeAndInsightDate(eq(MEMBER_ID), eq(MetricType.HEART_RATE), any()))
                .thenReturn(Optional.of(existing));

        service.generateDailyInsights(MEMBER_ID);

        verify(insightRepository).save(existing);
        assertThat(existing.getGeneratedText()).contains("lower");
        assertThat(existing.getKind()).isEqualTo(com.bonaca.backend.metrics.model.InsightKind.ANOMALY);
    }

    @Test
    void generatesARoutineConsistencyInsightWeightedTowardBehaviourOverVitals() {
        // screen_time and outdoor_time (behaviour, weight 0.6 combined) both deviate strongly;
        // sleep (vitals, weight 0.2) is right at baseline -> combined z should be dominated by behaviour.
        when(metricBaselineRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(
                baseline(MetricType.SCREEN_TIME, 4.0, 1.0),
                baseline(MetricType.OUTDOOR_TIME, 2.0, 0.5),
                baseline(MetricType.SLEEP, 7.0, 1.0)));
        when(metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(MEMBER_ID, MetricType.SCREEN_TIME))
                .thenReturn(Optional.of(reading(MetricType.SCREEN_TIME, 7.0))); // z = 3.0
        when(metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(MEMBER_ID, MetricType.OUTDOOR_TIME))
                .thenReturn(Optional.of(reading(MetricType.OUTDOOR_TIME, 3.5))); // z = 3.0
        when(metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(MEMBER_ID, MetricType.SLEEP))
                .thenReturn(Optional.of(reading(MetricType.SLEEP, 7.0))); // z = 0.0
        when(insightRepository.findByMemberIdAndMetricTypeIsNullAndInsightDate(eq(MEMBER_ID), any()))
                .thenReturn(Optional.empty());

        service.generateDailyInsights(MEMBER_ID);

        ArgumentCaptor<Insight> captor = ArgumentCaptor.forClass(Insight.class);
        verify(insightRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Insight compositeInsight = captor.getAllValues().stream()
                .filter(i -> i.getMetricType() == null)
                .findFirst()
                .orElseThrow();
        assertThat(compositeInsight.getGeneratedText()).contains("Routine Consistency").contains("Noticeably different");
    }

    @Test
    void doesNotGenerateATrendInsightWhenTheBaselineHasZeroSpread() {
        MetricBaseline baseline = baseline(MetricType.HEART_RATE, 70.0, 0.0);
        when(metricBaselineRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(baseline));

        service.generateDailyInsights(MEMBER_ID);

        verify(insightRepository, never()).save(any());
    }

    @Test
    void reRunningTheRoutineConsistencyInsightOnTheSameDayUpdatesTheExistingRowInsteadOfDuplicating() {
        when(metricBaselineRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(
                baseline(MetricType.SCREEN_TIME, 4.0, 1.0), baseline(MetricType.OUTDOOR_TIME, 2.0, 0.5)));
        when(metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(MEMBER_ID, MetricType.SCREEN_TIME))
                .thenReturn(Optional.of(reading(MetricType.SCREEN_TIME, 7.0)));
        when(metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(MEMBER_ID, MetricType.OUTDOOR_TIME))
                .thenReturn(Optional.of(reading(MetricType.OUTDOOR_TIME, 3.5)));
        Insight existingComposite =
                new Insight(MEMBER_ID, null, "stale composite text", com.bonaca.backend.metrics.model.InsightKind.TREND, LocalDate.now());
        when(insightRepository.findByMemberIdAndMetricTypeIsNullAndInsightDate(eq(MEMBER_ID), any()))
                .thenReturn(Optional.of(existingComposite));

        service.generateDailyInsights(MEMBER_ID);

        verify(insightRepository).save(existingComposite);
        assertThat(existingComposite.getGeneratedText()).contains("Routine Consistency");
    }

    @Test
    void generateDailyInsightsForAllMembersIteratesEveryDistinctMemberWithABaseline() {
        UUID otherMember = UUID.randomUUID();
        when(metricBaselineRepository.findAll()).thenReturn(List.of(
                baseline(MetricType.HEART_RATE, 70.0, 5.0),
                new MetricBaseline(otherMember, MetricType.STEPS, 8000.0, 500.0, 14, Instant.now())));
        when(metricBaselineRepository.findByMemberId(any())).thenReturn(List.of());

        service.generateDailyInsightsForAllMembers();

        verify(metricBaselineRepository).findByMemberId(MEMBER_ID);
        verify(metricBaselineRepository).findByMemberId(otherMember);
    }
}
