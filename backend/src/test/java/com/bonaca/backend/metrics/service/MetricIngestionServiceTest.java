package com.bonaca.backend.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.metrics.exception.SubscriptionInactiveException;
import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.MetricReadingRepository;
import com.bonaca.backend.subscriptions.service.SubscriptionService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricIngestionServiceTest {

    @Mock
    private MetricReadingRepository metricReadingRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SubscriptionService subscriptionService;

    private MetricIngestionService service;

    @BeforeEach
    void setUp() {
        service = new MetricIngestionService(metricReadingRepository, memberRepository, subscriptionService);
    }

    private static Member member(UUID accountId) {
        return new Member(accountId, UUID.randomUUID(), MemberRole.PRIMARY, "Name", null, null, null, null);
    }

    @Test
    void recordReadingSavesAReadingWithTheGivenFieldsWhenTheAccountIsActive() {
        UUID memberId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant recordedAt = Instant.now();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member(accountId)));
        when(subscriptionService.isActive(accountId)).thenReturn(true);
        when(metricReadingRepository.save(any(MetricReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MetricReading saved = service.recordReading(memberId, MetricType.HEART_RATE, 72.0, "bpm", recordedAt, "device-1");

        assertThat(saved.getMemberId()).isEqualTo(memberId);
        assertThat(saved.getMetricType()).isEqualTo(MetricType.HEART_RATE);
        assertThat(saved.getValue()).isEqualTo(72.0);
        assertThat(saved.getUnit()).isEqualTo("bpm");
        assertThat(saved.getRecordedAt()).isEqualTo(recordedAt);
        assertThat(saved.getSourceDeviceId()).isEqualTo("device-1");
    }

    @Test
    void recordReadingThrowsWhenTheAccountSubscriptionIsNotActive() {
        UUID memberId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member(accountId)));
        when(subscriptionService.isActive(accountId)).thenReturn(false);

        assertThatThrownBy(() -> service.recordReading(memberId, MetricType.HEART_RATE, 72.0, "bpm", Instant.now(), "device-1"))
                .isInstanceOf(SubscriptionInactiveException.class);
    }

    @Test
    void recordReadingThrowsWhenTheMemberDoesNotExist() {
        UUID memberId = UUID.randomUUID();
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordReading(memberId, MetricType.HEART_RATE, 72.0, "bpm", Instant.now(), "device-1"))
                .isInstanceOf(MemberNotFoundException.class);
    }
}
