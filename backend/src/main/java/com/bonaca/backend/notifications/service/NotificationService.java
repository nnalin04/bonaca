package com.bonaca.backend.notifications.service;

import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.notifications.exception.NotificationNotFoundException;
import com.bonaca.backend.notifications.exception.SelfPaymentRequestException;
import com.bonaca.backend.notifications.model.Notification;
import com.bonaca.backend.notifications.model.NotificationType;
import com.bonaca.backend.notifications.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-app notification storage and fan-out — see docs/TECHNICAL/NOTIFICATIONS_IMPLEMENTATION_PLAN.md
 * §2-3. {@link #create} is the entry point a future real Expo Push integration calls after
 * persisting (no SDK/HTTP call exists yet — CLAUDE.md's "Not Set Up Yet" blocker), mirroring
 * SubscriptionService.activate's relationship to a future payment webhook.
 *
 * <p>Authorization for the controller-facing methods is resolved here (requester userId in,
 * domain check inside), not in the controller — the same shape MetricsQueryService already
 * established, rather than SubscriptionsController's simpler inline equality check (a
 * single-field comparison there didn't justify a service round trip; resolving a second Member
 * and comparing accounts here does).
 */
@Service
public class NotificationService {

    /** PRD §7: flat ₹249/month, no per-account price to look up yet. */
    static final int PAYMENT_REQUEST_AMOUNT_RUPEES = 249;

    static final String PAYMENT_GATEWAY_DEEP_LINK = "/subscription/payment-gateway";

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final MemberPermissions permissions;

    public NotificationService(
            NotificationRepository notificationRepository, MemberRepository memberRepository, MemberPermissions permissions) {
        this.notificationRepository = notificationRepository;
        this.memberRepository = memberRepository;
        this.permissions = permissions;
    }

    @Transactional
    public Notification create(
            UUID recipientMemberId,
            UUID subjectMemberId,
            NotificationType type,
            String title,
            String body,
            String deepLinkTarget,
            UUID sourceInsightId) {
        Notification notification =
                new Notification(recipientMemberId, subjectMemberId, type, title, body, deepLinkTarget, sourceInsightId);
        return notificationRepository.save(notification);
    }

    /**
     * Called by SubscriptionLifecycleScheduler right after a TRIAL -> EXPIRED transition (see plan
     * doc §3.2/§4 for why subscriptions calls notifications, not the other way round). Idempotent
     * per account: skips a recipient who already has a SUBSCRIPTION notification, since EXPIRED is
     * currently a terminal state (no real payment processor to reactivate from it yet).
     */
    @Transactional
    public void notifyAccountMembers(UUID accountId, NotificationType type, String body, String deepLinkTarget) {
        for (Member member : memberRepository.findByAccountId(accountId)) {
            if (notificationRepository.existsByRecipientMemberIdAndType(member.getId(), type)) {
                continue;
            }
            create(member.getId(), member.getId(), type, displayName(member), body, deepLinkTarget, null);
        }
    }

    public List<Notification> listForMember(UUID requesterUserId, UUID memberId) {
        Member requester = permissions.requireMemberForUser(requesterUserId);
        if (!requester.getId().equals(memberId)) {
            throw new ForbiddenMemberAccessException("You can only view your own notifications");
        }
        return notificationRepository.findByRecipientMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Transactional
    public Notification markRead(UUID requesterUserId, UUID notificationId) {
        Member requester = permissions.requireMemberForUser(requesterUserId);
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));
        if (!notification.getRecipientMemberId().equals(requester.getId())) {
            throw new ForbiddenMemberAccessException("You don't have access to this notification");
        }
        notification.markRead();
        return notificationRepository.save(notification);
    }

    /** No money moves here — purely a message between two members of the same account, see plan doc §3.3. */
    @Transactional
    public Notification requestPayment(UUID requesterUserId, UUID recipientMemberId) {
        Member requester = permissions.requireMemberForUser(requesterUserId);
        if (requester.getId().equals(recipientMemberId)) {
            throw new SelfPaymentRequestException("You can't request payment from yourself");
        }
        Member recipient = memberRepository
                .findById(recipientMemberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found"));
        if (!requester.getAccountId().equals(recipient.getAccountId())) {
            throw new ForbiddenMemberAccessException("You don't have access to this member");
        }

        String body = displayName(requester) + " has requested ₹" + PAYMENT_REQUEST_AMOUNT_RUPEES
                + " to renew your wearable connection. Tap to pay.";
        return create(
                recipient.getId(),
                recipient.getId(),
                NotificationType.PAYMENT_REQUEST,
                displayName(recipient),
                body,
                PAYMENT_GATEWAY_DEEP_LINK,
                null);
    }

    private static String displayName(Member member) {
        return member.getNickname() != null ? member.getNickname() : member.getName();
    }
}
