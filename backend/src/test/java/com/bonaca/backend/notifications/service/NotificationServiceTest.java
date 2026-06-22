package com.bonaca.backend.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.notifications.exception.NotificationNotFoundException;
import com.bonaca.backend.notifications.exception.SelfPaymentRequestException;
import com.bonaca.backend.notifications.model.Notification;
import com.bonaca.backend.notifications.model.NotificationType;
import com.bonaca.backend.notifications.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPermissions permissions;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, memberRepository, permissions);
    }

    private static Member member(UUID accountId, String name, String nickname) {
        Member m = new Member(accountId, UUID.randomUUID(), MemberRole.SECONDARY, name, null, null, null, null);
        try {
            var nameField = Member.class.getDeclaredField("nickname");
            nameField.setAccessible(true);
            nameField.set(m, nickname);
            var idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(m, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    @Test
    void createPersistsANotificationWithTheGivenFields() {
        UUID recipientId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = service.create(
                recipientId, subjectId, NotificationType.METRIC_ANOMALY, "Dad", "Elevated heart rate", "/member/dad", insightId);

        assertThat(result.getRecipientMemberId()).isEqualTo(recipientId);
        assertThat(result.getSubjectMemberId()).isEqualTo(subjectId);
        assertThat(result.getType()).isEqualTo(NotificationType.METRIC_ANOMALY);
        assertThat(result.getSourceInsightId()).isEqualTo(insightId);
    }

    @Test
    void notifyAccountMembersCreatesOneSelfConcerningNotificationPerAccountMember() {
        UUID accountId = UUID.randomUUID();
        Member primary = member(accountId, "Asha Kumar", null);
        Member secondary = member(accountId, "Rakesh Kumar", null);
        when(memberRepository.findByAccountId(accountId)).thenReturn(List.of(primary, secondary));
        when(notificationRepository.existsByRecipientMemberIdAndType(any(), any())).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.notifyAccountMembers(accountId, NotificationType.SUBSCRIPTION, "Subscription lapsed.", "/subscription");

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void notifyAccountMembersSkipsAMemberWhoAlreadyHasOneOfThatType() {
        UUID accountId = UUID.randomUUID();
        Member primary = member(accountId, "Asha Kumar", null);
        when(memberRepository.findByAccountId(accountId)).thenReturn(List.of(primary));
        when(notificationRepository.existsByRecipientMemberIdAndType(primary.getId(), NotificationType.SUBSCRIPTION))
                .thenReturn(true);

        service.notifyAccountMembers(accountId, NotificationType.SUBSCRIPTION, "Subscription lapsed.", "/subscription");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void listForMemberReturnsTheRepositoryResultWhenTheRequesterIsTheMember() {
        UUID userId = UUID.randomUUID();
        Member requester = member(UUID.randomUUID(), "Asha Kumar", null);
        when(permissions.requireMemberForUser(userId)).thenReturn(requester);
        List<Notification> expected = List.of(new Notification(
                requester.getId(), requester.getId(), NotificationType.SUBSCRIPTION, "Me", "Body", "/subscription", null));
        when(notificationRepository.findByRecipientMemberIdOrderByCreatedAtDesc(requester.getId())).thenReturn(expected);

        assertThat(service.listForMember(userId, requester.getId())).isSameAs(expected);
    }

    @Test
    void listForMemberThrowsForbiddenWhenTheRequesterIsNotTheMember() {
        UUID userId = UUID.randomUUID();
        Member requester = member(UUID.randomUUID(), "Asha Kumar", null);
        when(permissions.requireMemberForUser(userId)).thenReturn(requester);

        assertThatThrownBy(() -> service.listForMember(userId, UUID.randomUUID()))
                .isInstanceOf(ForbiddenMemberAccessException.class);
    }

    @Test
    void requestPaymentUsesTheRequestersNameInTheBodyAndTheRecipientsNameAsTitle() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Member requester = member(accountId, "Rakesh Kumar", null);
        Member recipient = member(accountId, "Asha Kumar", "Mom");
        when(permissions.requireMemberForUser(userId)).thenReturn(requester);
        when(memberRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = service.requestPayment(userId, recipient.getId());

        assertThat(result.getRecipientMemberId()).isEqualTo(recipient.getId());
        assertThat(result.getSubjectMemberId()).isEqualTo(recipient.getId());
        assertThat(result.getType()).isEqualTo(NotificationType.PAYMENT_REQUEST);
        assertThat(result.getTitle()).isEqualTo("Mom");
        assertThat(result.getBody()).contains("Rakesh Kumar").contains("249");
        assertThat(result.getDeepLinkTarget()).isEqualTo("/subscription/payment-gateway");
    }

    @Test
    void requestPaymentThrowsWhenTargetingYourself() {
        UUID userId = UUID.randomUUID();
        Member requester = member(UUID.randomUUID(), "Rakesh Kumar", null);
        when(permissions.requireMemberForUser(userId)).thenReturn(requester);

        assertThatThrownBy(() -> service.requestPayment(userId, requester.getId()))
                .isInstanceOf(SelfPaymentRequestException.class);
    }

    @Test
    void requestPaymentThrowsForbiddenWhenTheRecipientIsNotInTheSameAccount() {
        UUID userId = UUID.randomUUID();
        Member requester = member(UUID.randomUUID(), "Rakesh Kumar", null);
        Member recipient = member(UUID.randomUUID(), "Stranger", null);
        when(permissions.requireMemberForUser(userId)).thenReturn(requester);
        when(memberRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));

        assertThatThrownBy(() -> service.requestPayment(userId, recipient.getId()))
                .isInstanceOf(ForbiddenMemberAccessException.class);
    }

    @Test
    void requestPaymentThrowsMemberNotFoundWhenTheRecipientDoesNotExist() {
        UUID userId = UUID.randomUUID();
        Member requester = member(UUID.randomUUID(), "Rakesh Kumar", null);
        UUID recipientId = UUID.randomUUID();
        when(permissions.requireMemberForUser(userId)).thenReturn(requester);
        when(memberRepository.findById(recipientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestPayment(userId, recipientId)).isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void markReadFlipsTheFlagWhenTheRequesterIsTheRecipient() {
        UUID userId = UUID.randomUUID();
        Member requester = member(UUID.randomUUID(), "Asha Kumar", null);
        when(permissions.requireMemberForUser(userId)).thenReturn(requester);
        Notification notification = new Notification(
                requester.getId(), requester.getId(), NotificationType.SUBSCRIPTION, "Me", "Body", "/subscription", null);
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        Notification result = service.markRead(userId, notificationId);

        assertThat(result.isRead()).isTrue();
    }

    @Test
    void markReadThrowsForbiddenWhenTheRequesterIsNotTheRecipient() {
        UUID userId = UUID.randomUUID();
        Member requester = member(UUID.randomUUID(), "Asha Kumar", null);
        when(permissions.requireMemberForUser(userId)).thenReturn(requester);
        Notification notification = new Notification(
                UUID.randomUUID(), UUID.randomUUID(), NotificationType.SUBSCRIPTION, "Me", "Body", "/subscription", null);
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markRead(userId, notificationId)).isInstanceOf(ForbiddenMemberAccessException.class);
    }

    @Test
    void markReadThrowsNotFoundWhenTheNotificationDoesNotExist() {
        UUID userId = UUID.randomUUID();
        Member requester = member(UUID.randomUUID(), "Asha Kumar", null);
        when(permissions.requireMemberForUser(userId)).thenReturn(requester);
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(userId, notificationId)).isInstanceOf(NotificationNotFoundException.class);
    }
}
