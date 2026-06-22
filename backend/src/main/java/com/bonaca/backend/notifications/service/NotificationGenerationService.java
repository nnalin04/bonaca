package com.bonaca.backend.notifications.service;

import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.SharingScope;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.metrics.model.Insight;
import com.bonaca.backend.metrics.model.InsightKind;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.repository.InsightRepository;
import com.bonaca.backend.notifications.model.NotificationType;
import com.bonaca.backend.notifications.repository.NotificationRepository;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The metric-anomaly and lapsed-subscription triggers — see
 * docs/TECHNICAL/NOTIFICATIONS_IMPLEMENTATION_PLAN.md §3.1/§3.2. Reads metrics.Insight and
 * subscriptions.Subscription (both read-only) plus members.Member/MemberPermissions; this is the
 * one-directional shape (notifications depends on the others, never the reverse) that avoids the
 * members -> subscriptions -> notifications -> members cycle a direct subscriptions -> notifications
 * call would have closed — see plan doc §4.
 */
@Service
public class NotificationGenerationService {

    static final String LAPSED_SUBSCRIPTION_BODY = "Health tracking is paused. Restart subscription to continue.";
    static final String PAYMENT_GATEWAY_DEEP_LINK = "/subscription/payment-gateway";

    private final InsightRepository insightRepository;
    private final MemberRepository memberRepository;
    private final MemberPermissions permissions;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final SubscriptionRepository subscriptionRepository;

    public NotificationGenerationService(
            InsightRepository insightRepository,
            MemberRepository memberRepository,
            MemberPermissions permissions,
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            SubscriptionRepository subscriptionRepository) {
        this.insightRepository = insightRepository;
        this.memberRepository = memberRepository;
        this.permissions = permissions;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.subscriptionRepository = subscriptionRepository;
    }

    /** Called by NotificationsRollupScheduler, after metrics' own nightly rollup has run. */
    @Transactional
    public void generateMetricAnomalyNotifications() {
        for (Insight insight : insightRepository.findByInsightDateAndKind(LocalDate.now(), InsightKind.ANOMALY)) {
            generateForInsight(insight);
        }
    }

    /**
     * Idempotent via NotificationService.notifyAccountMembers's own per-recipient-and-type check —
     * see plan doc §3.2 for why "once ever per account" is correct given EXPIRED is currently a
     * terminal state (no real payment processor to reactivate from it yet).
     */
    @Transactional
    public void generateLapsedSubscriptionNotifications() {
        for (var subscription : subscriptionRepository.findByStatus(SubscriptionStatus.EXPIRED)) {
            notificationService.notifyAccountMembers(
                    subscription.getAccountId(), NotificationType.SUBSCRIPTION, LAPSED_SUBSCRIPTION_BODY, PAYMENT_GATEWAY_DEEP_LINK);
        }
    }

    private void generateForInsight(Insight insight) {
        MetricType metricType = insight.getMetricType();
        if (metricType == null) {
            return; // the composite Routine Consistency insight never carries ANOMALY kind
        }
        Member subject = memberRepository.findById(insight.getMemberId()).orElse(null);
        if (subject == null) {
            return;
        }
        SharingScope scope = metricType.scope();
        String deepLinkTarget = "/member/" + subject.getId() + "/metric/" + metricType.name().toLowerCase();

        for (Member candidate : memberRepository.findByAccountId(subject.getAccountId())) {
            if (candidate.getId().equals(subject.getId())) {
                continue; // a member isn't notified about their own anomaly the way a Secondary watching them is
            }
            if (!permissions.canViewScope(candidate, subject, scope)) {
                continue;
            }
            if (notificationRepository.existsByRecipientMemberIdAndSourceInsightId(candidate.getId(), insight.getId())) {
                continue;
            }
            notificationService.create(
                    candidate.getId(),
                    subject.getId(),
                    NotificationType.METRIC_ANOMALY,
                    displayName(subject),
                    insight.getGeneratedText(),
                    deepLinkTarget,
                    insight.getId());
        }
    }

    private static String displayName(Member member) {
        return member.getNickname() != null ? member.getNickname() : member.getName();
    }
}
