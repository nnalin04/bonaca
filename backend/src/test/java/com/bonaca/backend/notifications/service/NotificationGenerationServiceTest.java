package com.bonaca.backend.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.model.SharingScope;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.metrics.model.Insight;
import com.bonaca.backend.metrics.model.InsightKind;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.InsightRepository;
import com.bonaca.backend.notifications.model.NotificationType;
import com.bonaca.backend.notifications.repository.NotificationRepository;
import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationGenerationServiceTest {

    @Mock
    private InsightRepository insightRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPermissions permissions;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private NotificationGenerationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationGenerationService(
                insightRepository, memberRepository, permissions, notificationRepository, notificationService, subscriptionRepository);
    }

    private static Member member(UUID accountId) {
        Member m = new Member(accountId, UUID.randomUUID(), MemberRole.SECONDARY, "Name", null, null, null, null);
        try {
            var idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(m, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    private static Insight insight(UUID memberId, MetricType metricType) {
        Insight i = new Insight(memberId, metricType, "Heart rate has been higher than usual today.", InsightKind.ANOMALY, LocalDate.now());
        try {
            var idField = Insight.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(i, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return i;
    }

    @Test
    void notifiesAnAccountMemberWhoCanViewTheMetricsScope() {
        UUID accountId = UUID.randomUUID();
        Member subject = member(accountId);
        Member secondary = member(accountId);
        Insight anomaly = insight(subject.getId(), MetricType.HEART_RATE);
        when(insightRepository.findByInsightDateAndKind(any(), eq(InsightKind.ANOMALY))).thenReturn(List.of(anomaly));
        when(memberRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(memberRepository.findByAccountId(accountId)).thenReturn(List.of(subject, secondary));
        when(permissions.canViewScope(secondary, subject, SharingScope.VITALS)).thenReturn(true);
        when(notificationRepository.existsByRecipientMemberIdAndSourceInsightId(secondary.getId(), anomaly.getId()))
                .thenReturn(false);

        service.generateMetricAnomalyNotifications();

        verify(notificationService)
                .create(
                        eq(secondary.getId()),
                        eq(subject.getId()),
                        eq(NotificationType.METRIC_ANOMALY),
                        any(),
                        eq(anomaly.getGeneratedText()),
                        any(),
                        eq(anomaly.getId()));
    }

    @Test
    void doesNotNotifyAnAccountMemberWithoutScopeVisibility() {
        UUID accountId = UUID.randomUUID();
        Member subject = member(accountId);
        Member secondary = member(accountId);
        Insight anomaly = insight(subject.getId(), MetricType.HEART_RATE);
        when(insightRepository.findByInsightDateAndKind(any(), eq(InsightKind.ANOMALY))).thenReturn(List.of(anomaly));
        when(memberRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(memberRepository.findByAccountId(accountId)).thenReturn(List.of(subject, secondary));
        when(permissions.canViewScope(secondary, subject, SharingScope.VITALS)).thenReturn(false);

        service.generateMetricAnomalyNotifications();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void doesNotNotifyTheSubjectAboutTheirOwnAnomaly() {
        UUID accountId = UUID.randomUUID();
        Member subject = member(accountId);
        Insight anomaly = insight(subject.getId(), MetricType.HEART_RATE);
        when(insightRepository.findByInsightDateAndKind(any(), eq(InsightKind.ANOMALY))).thenReturn(List.of(anomaly));
        when(memberRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(memberRepository.findByAccountId(accountId)).thenReturn(List.of(subject));

        service.generateMetricAnomalyNotifications();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void isIdempotentWhenANotificationAlreadyExistsForThatRecipientAndInsight() {
        UUID accountId = UUID.randomUUID();
        Member subject = member(accountId);
        Member secondary = member(accountId);
        Insight anomaly = insight(subject.getId(), MetricType.HEART_RATE);
        when(insightRepository.findByInsightDateAndKind(any(), eq(InsightKind.ANOMALY))).thenReturn(List.of(anomaly));
        when(memberRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(memberRepository.findByAccountId(accountId)).thenReturn(List.of(subject, secondary));
        when(permissions.canViewScope(secondary, subject, SharingScope.VITALS)).thenReturn(true);
        when(notificationRepository.existsByRecipientMemberIdAndSourceInsightId(secondary.getId(), anomaly.getId()))
                .thenReturn(true);

        service.generateMetricAnomalyNotifications();

        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void skipsTheCompositeRoutineConsistencyInsightWhichHasNoMetricType() {
        Insight composite = insight(UUID.randomUUID(), null);
        when(insightRepository.findByInsightDateAndKind(any(), eq(InsightKind.ANOMALY))).thenReturn(List.of(composite));

        service.generateMetricAnomalyNotifications();

        verify(memberRepository, never()).findById(any());
        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void generateLapsedSubscriptionNotificationsNotifiesEveryExpiredAccount() {
        Subscription expired = new Subscription(UUID.randomUUID(), SubscriptionStatus.EXPIRED, null);
        when(subscriptionRepository.findByStatus(SubscriptionStatus.EXPIRED)).thenReturn(List.of(expired));

        service.generateLapsedSubscriptionNotifications();

        verify(notificationService)
                .notifyAccountMembers(
                        eq(expired.getAccountId()),
                        eq(NotificationType.SUBSCRIPTION),
                        any(),
                        eq("/subscription/payment-gateway"));
    }

    @Test
    void generateLapsedSubscriptionNotificationsDoesNothingWhenNoAccountIsExpired() {
        when(subscriptionRepository.findByStatus(SubscriptionStatus.EXPIRED)).thenReturn(List.of());

        service.generateLapsedSubscriptionNotifications();

        verify(notificationService, never()).notifyAccountMembers(any(), any(), any(), any());
    }
}
