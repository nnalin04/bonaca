package com.bonaca.backend.members.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bonaca.backend.auth.model.User;
import com.bonaca.backend.auth.repository.UserRepository;
import com.bonaca.backend.members.dto.CompleteProfileRequest;
import com.bonaca.backend.members.dto.MemberResponse;
import com.bonaca.backend.members.dto.UpdateMemberRequest;
import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.exception.MemberLimitExceededException;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Account;
import com.bonaca.backend.members.model.Invite;
import com.bonaca.backend.members.model.InviteStatus;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.model.SharingGrant;
import com.bonaca.backend.members.repository.AccountRepository;
import com.bonaca.backend.members.repository.InviteRepository;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.repository.SharingGrantRepository;
import com.bonaca.backend.subscriptions.service.SubscriptionService;
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
 * MembersService orchestrates the onboarding (complete-profile) flow and member visibility per
 * its own class/method Javadoc: a first-time completion creates a fresh Account + Primary
 * Member + 7-day TRIAL subscription; completing against a pending Invite instead attaches the
 * user under the inviter's account at the offered role and grants the new Secondary all scopes
 * from the Primary by default (docs/PRD.pdf §11.1), capped at 2 Secondary Members.
 * MemberPermissions' own authorization rules are unit-tested separately in
 * MemberPermissionsTest — here it's mocked so these tests stay focused on MembersService's own
 * orchestration logic.
 */
@ExtendWith(MockitoExtension.class)
class MembersServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PHONE = "+919876543210";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private SharingGrantRepository sharingGrantRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberPermissions permissions;

    private MembersService membersService;

    @BeforeEach
    void setUp() {
        membersService = new MembersService(
                accountRepository, memberRepository, inviteRepository, sharingGrantRepository, subscriptionService,
                userRepository, permissions);
    }

    private static void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Member member(UUID id, UUID accountId, MemberRole role) {
        Member m = new Member(accountId, UUID.randomUUID(), role, "Name", null, null, null, null);
        setId(m, id);
        return m;
    }

    // ---- completeProfile ----

    @Test
    void completeProfileThrowsWhenProfileAlreadyExists() {
        when(memberRepository.findByUserId(USER_ID)).thenReturn(Optional.of(member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY)));

        assertThatThrownBy(() -> membersService.completeProfile(USER_ID, new CompleteProfileRequest("Name", null, null, null, null)))
                .isInstanceOf(ForbiddenMemberAccessException.class);
    }

    @Test
    void completeProfileThrowsWhenUserNoLongerExists() {
        when(memberRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> membersService.completeProfile(USER_ID, new CompleteProfileRequest("Name", null, null, null, null)))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void completeProfileWithNoPendingInviteCreatesAccountPrimaryMemberAndTrialSubscription() {
        User user = new User(PHONE);
        when(memberRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(PHONE, InviteStatus.PENDING))
                .thenReturn(Optional.empty());

        UUID accountId = UUID.randomUUID();
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            setId(a, accountId);
            return a;
        });
        UUID memberId = UUID.randomUUID();
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            setId(m, memberId);
            return m;
        });

        MemberResponse response =
                membersService.completeProfile(USER_ID, new CompleteProfileRequest("Asha Kumar", "female", null, null, null));

        assertThat(response.role()).isEqualTo("primary");
        assertThat(response.self()).isTrue();
        assertThat(response.accountId()).isEqualTo(accountId);
        assertThat(user.isProfileCompleted()).isTrue();
        verify(userRepository).save(user);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getOwnerMemberId()).isEqualTo(memberId);

        // What "starting a trial" actually means (status, 7-day window) is SubscriptionService's
        // own concern, tested in SubscriptionServiceTest — here we only verify it's triggered
        // for the right account, at the right moment.
        verify(subscriptionService).startTrial(accountId);
    }

    @Test
    void completeProfileWithPendingInviteAttachesOfferedRoleAndGrantsAllScopesFromThePrimary() {
        User user = new User(PHONE);
        when(memberRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UUID inviterAccountId = UUID.randomUUID();
        Invite invite = new Invite(inviterAccountId, PHONE, MemberRole.SECONDARY);
        setId(invite, UUID.randomUUID());
        when(inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(PHONE, InviteStatus.PENDING))
                .thenReturn(Optional.of(invite));

        Member existingPrimary = member(UUID.randomUUID(), inviterAccountId, MemberRole.PRIMARY);
        when(memberRepository.findByAccountId(inviterAccountId)).thenReturn(List.of(existingPrimary));

        UUID newMemberId = UUID.randomUUID();
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            setId(m, newMemberId);
            return m;
        });

        MemberResponse response =
                membersService.completeProfile(USER_ID, new CompleteProfileRequest("Rakesh Kumar", "male", null, null, null));

        assertThat(response.role()).isEqualTo("secondary");
        assertThat(response.accountId()).isEqualTo(inviterAccountId);

        assertThat(invite.getStatus()).isEqualTo(InviteStatus.ACCEPTED);
        assertThat(invite.getAcceptedMemberId()).isEqualTo(newMemberId);
        verify(inviteRepository).save(invite);

        ArgumentCaptor<SharingGrant> grantCaptor = ArgumentCaptor.forClass(SharingGrant.class);
        verify(sharingGrantRepository, times(3)).save(grantCaptor.capture());
        assertThat(grantCaptor.getAllValues())
                .allMatch(g -> g.getGranterMemberId().equals(existingPrimary.getId())
                        && g.getGranteeMemberId().equals(newMemberId)
                        && g.isVisible());

        verify(accountRepository, never()).save(any());
        verify(subscriptionService, never()).startTrial(any());
    }

    @Test
    void completeProfileRejectsAThirdSecondaryMemberOnTheSameAccount() {
        User user = new User(PHONE);
        when(memberRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UUID inviterAccountId = UUID.randomUUID();
        Invite invite = new Invite(inviterAccountId, PHONE, MemberRole.SECONDARY);
        setId(invite, UUID.randomUUID());
        when(inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(PHONE, InviteStatus.PENDING))
                .thenReturn(Optional.of(invite));

        List<Member> existingMembers = List.of(
                member(UUID.randomUUID(), inviterAccountId, MemberRole.PRIMARY),
                member(UUID.randomUUID(), inviterAccountId, MemberRole.SECONDARY),
                member(UUID.randomUUID(), inviterAccountId, MemberRole.SECONDARY));
        when(memberRepository.findByAccountId(inviterAccountId)).thenReturn(existingMembers);

        assertThatThrownBy(() -> membersService.completeProfile(USER_ID, new CompleteProfileRequest("Name", null, null, null, null)))
                .isInstanceOf(MemberLimitExceededException.class);

        verify(memberRepository, never()).save(any());
    }

    // ---- listVisibleMembers ----

    @Test
    void listVisibleMembersForAPrimaryReturnsAllAccountMembersSortedPinnedFirst() {
        UUID accountId = UUID.randomUUID();
        Member primary = member(UUID.randomUUID(), accountId, MemberRole.PRIMARY);
        Member secondary = member(UUID.randomUUID(), accountId, MemberRole.SECONDARY);
        secondary.setPinned(true);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(primary);
        when(memberRepository.findByAccountId(accountId)).thenReturn(List.of(primary, secondary));

        List<MemberResponse> result = membersService.listVisibleMembers(USER_ID);

        assertThat(result).extracting(MemberResponse::id).containsExactly(secondary.getId(), primary.getId());
        assertThat(result).filteredOn(r -> r.id().equals(primary.getId())).first().extracting(MemberResponse::self).isEqualTo(true);
    }

    @Test
    void listVisibleMembersForASecondaryReturnsGrantedMembersPlusSelf() {
        Member self = member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        Member granter = member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(self);
        SharingGrant grant = new SharingGrant(granter.getId(), self.getId(), com.bonaca.backend.members.model.SharingScope.VITALS, true);
        when(sharingGrantRepository.findByGranteeMemberIdAndVisibleTrue(self.getId())).thenReturn(List.of(grant));
        when(memberRepository.findByIdIn(List.of(granter.getId()))).thenReturn(List.of(granter));

        List<MemberResponse> result = membersService.listVisibleMembers(USER_ID);

        assertThat(result).extracting(MemberResponse::id).containsExactlyInAnyOrder(self.getId(), granter.getId());
    }

    // ---- getMember ----

    @Test
    void getMemberReturnsTheTargetWhenViewable() {
        Member me = member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);
        Member target = member(UUID.randomUUID(), me.getAccountId(), MemberRole.SECONDARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canView(me, target)).thenReturn(true);

        MemberResponse response = membersService.getMember(USER_ID, target.getId());

        assertThat(response.id()).isEqualTo(target.getId());
        assertThat(response.self()).isFalse();
    }

    @Test
    void getMemberThrowsNotFoundWhenTargetDoesNotExist() {
        Member me = member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);
        UUID missingId = UUID.randomUUID();
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        when(memberRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> membersService.getMember(USER_ID, missingId)).isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void getMemberThrowsForbiddenWhenNotViewable() {
        Member me = member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        Member target = member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(permissions.canView(me, target)).thenReturn(false);

        assertThatThrownBy(() -> membersService.getMember(USER_ID, target.getId())).isInstanceOf(ForbiddenMemberAccessException.class);
    }

    // ---- updateMember ----

    @Test
    void updateMemberOnlyAppliesProvidedFields() {
        Member me = member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);
        Member target = member(UUID.randomUUID(), me.getAccountId(), MemberRole.SECONDARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(memberRepository.save(target)).thenReturn(target);

        MemberResponse response = membersService.updateMember(USER_ID, target.getId(), new UpdateMemberRequest("Bro", null, null));

        assertThat(response.nickname()).isEqualTo("Bro");
        assertThat(response.hidden()).isFalse();
        verify(permissions).requireAccountOwner(me, target.getAccountId());
    }

    @Test
    void updateMemberAppliesPinnedAndHiddenWhenProvided() {
        Member me = member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);
        Member target = member(UUID.randomUUID(), me.getAccountId(), MemberRole.SECONDARY);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        when(memberRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(memberRepository.save(target)).thenReturn(target);

        MemberResponse response = membersService.updateMember(USER_ID, target.getId(), new UpdateMemberRequest(null, true, true));

        assertThat(response.pinned()).isTrue();
        assertThat(response.hidden()).isTrue();
        assertThat(response.nickname()).isNull();
    }

    @Test
    void updateMemberThrowsNotFoundWhenTargetDoesNotExist() {
        Member me = member(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);
        UUID missingId = UUID.randomUUID();
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        when(memberRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> membersService.updateMember(USER_ID, missingId, new UpdateMemberRequest("X", null, null)))
                .isInstanceOf(MemberNotFoundException.class);
    }
}
