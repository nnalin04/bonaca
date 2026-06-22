package com.bonaca.backend.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.model.SharingScope;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.metrics.dto.InsightResponse;
import com.bonaca.backend.metrics.dto.MetricDetailResponse;
import com.bonaca.backend.metrics.dto.MetricRange;
import com.bonaca.backend.metrics.dto.MetricSummaryResponse;
import com.bonaca.backend.metrics.model.Insight;
import com.bonaca.backend.metrics.model.InsightKind;
import com.bonaca.backend.metrics.model.MetricBaseline;
import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.InsightRepository;
import com.bonaca.backend.metrics.repository.MetricBaselineRepository;
import com.bonaca.backend.metrics.repository.MetricReadingRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsQueryServiceTest {

    private static final UUID REQUESTER_USER_ID = UUID.randomUUID();

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPermissions permissions;

    @Mock
    private MetricReadingRepository metricReadingRepository;

    @Mock
    private MetricBaselineRepository metricBaselineRepository;

    @Mock
    private InsightRepository insightRepository;

    private MetricsQueryService service;

    @BeforeEach
    void setUp() {
        service = new MetricsQueryService(
                memberRepository, permissions, metricReadingRepository, metricBaselineRepository, insightRepository);
    }

    private static Member member(UUID accountId) {
        Member m = new Member(accountId, UUID.randomUUID(), MemberRole.SECONDARY, "Name", null, null, null, null);
        try {
            var field = Member.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(m, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    private static MetricReading reading(UUID memberId, MetricType type, double value) {
        return new MetricReading(memberId, type, value, "unit", Instant.now(), "device-1");
    }

    // ---- getMemberMetricsSummary ----

    @Test
    void summaryThrowsWhenTheMemberDoesNotExist() {
        Member requester = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        UUID missingId = UUID.randomUUID();
        when(memberRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMemberMetricsSummary(REQUESTER_USER_ID, missingId, MetricRange.SEVEN_DAYS))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void summaryThrowsWhenTheRequesterCannotViewTheMemberAtAll() {
        Member requester = member(UUID.randomUUID());
        Member target = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canView(requester, target)).thenReturn(false);

        assertThatThrownBy(() -> service.getMemberMetricsSummary(REQUESTER_USER_ID, target.getId(), MetricRange.SEVEN_DAYS))
                .isInstanceOf(ForbiddenMemberAccessException.class);
    }

    @Test
    void summaryOmitsMetricTypesTheRequesterLacksScopeAccessFor() {
        Member requester = member(UUID.randomUUID());
        Member target = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canView(requester, target)).thenReturn(true);
        when(metricReadingRepository.findByMemberIdAndRecordedAtAfterOrderByRecordedAtAsc(any(), any())).thenReturn(List.of(
                reading(target.getId(), MetricType.HEART_RATE, 75.0),
                reading(target.getId(), MetricType.SCREEN_TIME, 5.0)));
        when(permissions.canViewScope(requester, target, SharingScope.VITALS)).thenReturn(true);
        when(permissions.canViewScope(requester, target, SharingScope.BEHAVIOUR)).thenReturn(false);
        when(metricBaselineRepository.findByMemberIdAndMetricType(any(), any())).thenReturn(Optional.empty());

        List<MetricSummaryResponse> result = service.getMemberMetricsSummary(REQUESTER_USER_ID, target.getId(), MetricRange.SEVEN_DAYS);

        assertThat(result).extracting(MetricSummaryResponse::metricType).containsExactly("heart_rate");
    }

    @Test
    void summarySortsByDeviationFromBaselineDescending() {
        Member requester = member(UUID.randomUUID());
        Member target = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canView(requester, target)).thenReturn(true);
        when(permissions.canViewScope(any(), any(), any())).thenReturn(true);
        when(metricReadingRepository.findByMemberIdAndRecordedAtAfterOrderByRecordedAtAsc(any(), any())).thenReturn(List.of(
                reading(target.getId(), MetricType.HEART_RATE, 75.0), // baseline 70/5 -> z=1.0
                reading(target.getId(), MetricType.STEPS, 12000.0))); // baseline 8000/500 -> z=8.0
        when(metricBaselineRepository.findByMemberIdAndMetricType(target.getId(), MetricType.HEART_RATE))
                .thenReturn(Optional.of(new MetricBaseline(target.getId(), MetricType.HEART_RATE, 70.0, 5.0, 14, Instant.now())));
        when(metricBaselineRepository.findByMemberIdAndMetricType(target.getId(), MetricType.STEPS))
                .thenReturn(Optional.of(new MetricBaseline(target.getId(), MetricType.STEPS, 8000.0, 500.0, 14, Instant.now())));

        List<MetricSummaryResponse> result = service.getMemberMetricsSummary(REQUESTER_USER_ID, target.getId(), MetricRange.SEVEN_DAYS);

        assertThat(result).extracting(MetricSummaryResponse::metricType).containsExactly("steps", "heart_rate");
    }

    // ---- getMetricDetail ----

    @Test
    void detailReturnsNoDataShapeWhenThereAreZeroReadings() {
        Member requester = member(UUID.randomUUID());
        Member target = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canViewScope(requester, target, SharingScope.VITALS)).thenReturn(true);
        when(metricReadingRepository.findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of());

        MetricDetailResponse response =
                service.getMetricDetail(REQUESTER_USER_ID, target.getId(), MetricType.HEART_RATE, MetricRange.TWENTY_FOUR_HOURS);

        assertThat(response.hasData()).isFalse();
        assertThat(response.average()).isNull();
        assertThat(response.chartValues()).isEmpty();
    }

    @Test
    void detailComputesTheLiveAverageAndTrendAgainstTheCachedBaseline() {
        Member requester = member(UUID.randomUUID());
        Member target = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canViewScope(requester, target, SharingScope.VITALS)).thenReturn(true);
        when(metricReadingRepository.findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of(
                        reading(target.getId(), MetricType.HEART_RATE, 90.0),
                        reading(target.getId(), MetricType.HEART_RATE, 90.0)));
        when(metricBaselineRepository.findByMemberIdAndMetricType(target.getId(), MetricType.HEART_RATE))
                .thenReturn(Optional.of(new MetricBaseline(target.getId(), MetricType.HEART_RATE, 70.0, 5.0, 14, Instant.now())));
        when(insightRepository.findByMemberIdAndMetricTypeAndInsightDate(target.getId(), MetricType.HEART_RATE, LocalDate.now()))
                .thenReturn(Optional.of(new com.bonaca.backend.metrics.model.Insight(
                        target.getId(), MetricType.HEART_RATE, "Heart rate has been higher than usual today.",
                        com.bonaca.backend.metrics.model.InsightKind.TREND, LocalDate.now())));

        MetricDetailResponse response =
                service.getMetricDetail(REQUESTER_USER_ID, target.getId(), MetricType.HEART_RATE, MetricRange.TWENTY_FOUR_HOURS);

        assertThat(response.hasData()).isTrue();
        assertThat(response.average()).isEqualTo(90.0);
        assertThat(response.trendLabel()).isEqualTo("higher_than_usual");
        assertThat(response.insightText()).isEqualTo("Heart rate has been higher than usual today.");
    }

    @Test
    void detailHasNoTrendLabelWhenTheBaselineHasZeroSpread() {
        Member requester = member(UUID.randomUUID());
        Member target = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canViewScope(requester, target, SharingScope.VITALS)).thenReturn(true);
        when(metricReadingRepository.findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of(reading(target.getId(), MetricType.HEART_RATE, 70.0)));
        when(metricBaselineRepository.findByMemberIdAndMetricType(target.getId(), MetricType.HEART_RATE))
                .thenReturn(Optional.of(new MetricBaseline(target.getId(), MetricType.HEART_RATE, 70.0, 0.0, 14, Instant.now())));
        when(insightRepository.findByMemberIdAndMetricTypeAndInsightDate(target.getId(), MetricType.HEART_RATE, LocalDate.now()))
                .thenReturn(Optional.empty());

        MetricDetailResponse response =
                service.getMetricDetail(REQUESTER_USER_ID, target.getId(), MetricType.HEART_RATE, MetricRange.TWENTY_FOUR_HOURS);

        assertThat(response.trendLabel()).isNull();
    }

    @Test
    void detailThrowsWhenTheRequesterLacksScopeAccessForThatMetric() {
        Member requester = member(UUID.randomUUID());
        Member target = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canViewScope(requester, target, SharingScope.VITALS)).thenReturn(false);

        assertThatThrownBy(() ->
                        service.getMetricDetail(REQUESTER_USER_ID, target.getId(), MetricType.HEART_RATE, MetricRange.SEVEN_DAYS))
                .isInstanceOf(ForbiddenMemberAccessException.class);
    }

    // ---- listInsights ----

    @Test
    void listInsightsFiltersOutMetricSpecificInsightsTheRequesterLacksScopeFor() {
        Member requester = member(UUID.randomUUID());
        Member target = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canView(requester, target)).thenReturn(true);
        Insight vitalsInsight = new Insight(target.getId(), MetricType.HEART_RATE, "text", InsightKind.TREND, LocalDate.now());
        Insight compositeInsight = new Insight(target.getId(), null, "Routine Consistency: Stable", InsightKind.TREND, LocalDate.now());
        when(insightRepository.findByMemberIdOrderByInsightDateDesc(target.getId()))
                .thenReturn(List.of(vitalsInsight, compositeInsight));
        when(permissions.canViewScope(requester, target, SharingScope.VITALS)).thenReturn(false);

        List<InsightResponse> result = service.listInsights(REQUESTER_USER_ID, target.getId());

        assertThat(result).extracting(InsightResponse::metricType).containsExactly((String) null);
    }

    @Test
    void listInsightsIncludesMetricSpecificInsightsTheRequesterHasScopeAccessFor() {
        Member requester = member(UUID.randomUUID());
        Member target = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(REQUESTER_USER_ID)).thenReturn(requester);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canView(requester, target)).thenReturn(true);
        Insight vitalsInsight = new Insight(target.getId(), MetricType.HEART_RATE, "Heart rate text", InsightKind.TREND, LocalDate.now());
        when(insightRepository.findByMemberIdOrderByInsightDateDesc(target.getId())).thenReturn(List.of(vitalsInsight));
        when(permissions.canViewScope(requester, target, SharingScope.VITALS)).thenReturn(true);

        List<InsightResponse> result = service.listInsights(REQUESTER_USER_ID, target.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).metricType()).isEqualTo("heart_rate");
        assertThat(result.get(0).generatedText()).isEqualTo("Heart rate text");
    }
}
