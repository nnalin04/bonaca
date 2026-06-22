package com.bonaca.backend.members.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bonaca.backend.members.dto.InviteResponse;
import com.bonaca.backend.members.exception.MemberLimitExceededException;
import com.bonaca.backend.members.model.Invite;
import com.bonaca.backend.members.model.InviteStatus;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.repository.InviteRepository;
import com.bonaca.backend.members.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * InviteService.create's contract (see its own Javadoc + MAX_SECONDARY_MEMBERS = 2): re-posting
 * an invite to a phone number that already has a pending invite from the *same* inviter is
 * idempotent (returns the existing invite, no duplicate), but the 2-Secondary-Member cap counts
 * existing Secondary Members *and* other pending invites together.
 */
@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PHONE = "+919876543210";

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPermissions permissions;

    private InviteService inviteService;

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(inviteRepository, memberRepository, permissions);
    }

    private static Member member(UUID accountId, MemberRole role) {
        Member m = new Member(accountId, UUID.randomUUID(), role, "Name", null, null, null, null);
        try {
            var field = Member.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(m, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    @Test
    void createReusesAnExistingPendingInviteFromTheSameInviter() {
        Member me = member(UUID.randomUUID(), MemberRole.PRIMARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        Invite existing = new Invite(me.getAccountId(), PHONE, MemberRole.SECONDARY);
        when(inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(PHONE, InviteStatus.PENDING))
                .thenReturn(Optional.of(existing));

        InviteResponse response = inviteService.create(USER_ID, PHONE);

        assertThat(response.phoneNumber()).isEqualTo(PHONE);
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void createIgnoresAPendingInviteBelongingToADifferentInviterAccount() {
        Member me = member(UUID.randomUUID(), MemberRole.PRIMARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        Invite existingElsewhere = new Invite(UUID.randomUUID(), PHONE, MemberRole.SECONDARY);
        when(inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(PHONE, InviteStatus.PENDING))
                .thenReturn(Optional.of(existingElsewhere));
        when(memberRepository.findByAccountId(me.getAccountId())).thenReturn(List.of());
        when(inviteRepository.findByInviterAccountId(me.getAccountId())).thenReturn(List.of());
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InviteResponse response = inviteService.create(USER_ID, PHONE);

        assertThat(response.phoneNumber()).isEqualTo(PHONE);
        verify(inviteRepository).save(any(Invite.class));
    }

    @Test
    void createSavesANewInviteWhenNoneIsPending() {
        Member me = member(UUID.randomUUID(), MemberRole.PRIMARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        when(inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(PHONE, InviteStatus.PENDING))
                .thenReturn(Optional.empty());
        when(memberRepository.findByAccountId(me.getAccountId())).thenReturn(List.of());
        when(inviteRepository.findByInviterAccountId(me.getAccountId())).thenReturn(List.of());
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InviteResponse response = inviteService.create(USER_ID, PHONE);

        assertThat(response.phoneNumber()).isEqualTo(PHONE);
        assertThat(response.roleOffered()).isEqualTo("secondary");
        assertThat(response.status()).isEqualTo("pending");
    }

    @Test
    void createCountsExistingSecondaryMembersAndPendingInvitesTowardTheCap() {
        Member me = member(UUID.randomUUID(), MemberRole.PRIMARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        when(inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(PHONE, InviteStatus.PENDING))
                .thenReturn(Optional.empty());
        when(memberRepository.findByAccountId(me.getAccountId()))
                .thenReturn(List.of(member(me.getAccountId(), MemberRole.SECONDARY)));
        Invite pending = new Invite(me.getAccountId(), "+919999999999", MemberRole.SECONDARY);
        when(inviteRepository.findByInviterAccountId(me.getAccountId())).thenReturn(List.of(pending));

        assertThatThrownBy(() -> inviteService.create(USER_ID, PHONE)).isInstanceOf(MemberLimitExceededException.class);

        verify(inviteRepository, never()).save(any());
    }

    @Test
    void listForCurrentAccountReturnsInvitesForTheOwnedAccount() {
        Member me = member(UUID.randomUUID(), MemberRole.PRIMARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        Invite invite = new Invite(me.getAccountId(), PHONE, MemberRole.SECONDARY);
        when(inviteRepository.findByInviterAccountId(me.getAccountId())).thenReturn(List.of(invite));

        List<InviteResponse> result = inviteService.listForCurrentAccount(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).phoneNumber()).isEqualTo(PHONE);
        verify(permissions).requireAccountOwner(me, me.getAccountId());
    }
}
