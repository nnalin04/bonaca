package com.bonaca.backend.members.service;

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
import com.bonaca.backend.members.repository.SharingGrantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MemberPermissions is the actual authorization layer for members/sharing (see its class
 * Javadoc) — these tests pin the three rules it enforces: a user must have completed their
 * profile to act at all, only the account-owning Primary can manage account-wide things, and
 * viewing another member's data requires self/Primary-same-account/an explicit visible grant.
 */
@ExtendWith(MockitoExtension.class)
class MemberPermissionsTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SharingGrantRepository sharingGrantRepository;

    private MemberPermissions permissions;

    private MemberPermissions newPermissions() {
        return new MemberPermissions(memberRepository, sharingGrantRepository);
    }

    private static Member memberWithId(UUID id, UUID accountId, MemberRole role) {
        Member member = new Member(accountId, UUID.randomUUID(), role, "Name", null, null, null, null);
        try {
            var field = Member.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(member, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return member;
    }

    @Test
    void requireMemberForUserReturnsTheMatchingMember() {
        permissions = newPermissions();
        UUID userId = UUID.randomUUID();
        Member member = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);
        when(memberRepository.findByUserId(userId)).thenReturn(Optional.of(member));

        assertThat(permissions.requireMemberForUser(userId)).isSameAs(member);
    }

    @Test
    void requireMemberForUserThrowsWhenProfileNotCompleted() {
        permissions = newPermissions();
        UUID userId = UUID.randomUUID();
        when(memberRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissions.requireMemberForUser(userId)).isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void requireAccountOwnerAllowsThePrimaryOfTheMatchingAccount() {
        permissions = newPermissions();
        UUID accountId = UUID.randomUUID();
        Member primary = memberWithId(UUID.randomUUID(), accountId, MemberRole.PRIMARY);

        permissions.requireAccountOwner(primary, accountId);
    }

    @Test
    void requireAccountOwnerRejectsASecondaryMember() {
        permissions = newPermissions();
        UUID accountId = UUID.randomUUID();
        Member secondary = memberWithId(UUID.randomUUID(), accountId, MemberRole.SECONDARY);

        assertThatThrownBy(() -> permissions.requireAccountOwner(secondary, accountId))
                .isInstanceOf(ForbiddenMemberAccessException.class);
    }

    @Test
    void requireAccountOwnerRejectsAPrimaryOfADifferentAccount() {
        permissions = newPermissions();
        Member primaryElsewhere = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);

        assertThatThrownBy(() -> permissions.requireAccountOwner(primaryElsewhere, UUID.randomUUID()))
                .isInstanceOf(ForbiddenMemberAccessException.class);
    }

    @Test
    void canViewIsTrueForOnesOwnMemberRow() {
        permissions = newPermissions();
        UUID id = UUID.randomUUID();
        Member self = memberWithId(id, UUID.randomUUID(), MemberRole.SECONDARY);

        assertThat(permissions.canView(self, self)).isTrue();
    }

    @Test
    void canViewIsTrueForThePrimaryViewingAnyMemberInTheSameAccount() {
        permissions = newPermissions();
        UUID accountId = UUID.randomUUID();
        Member primary = memberWithId(UUID.randomUUID(), accountId, MemberRole.PRIMARY);
        Member other = memberWithId(UUID.randomUUID(), accountId, MemberRole.SECONDARY);

        assertThat(permissions.canView(primary, other)).isTrue();
    }

    @Test
    void canViewIsFalseForAPrimaryOfADifferentAccountWithoutAGrant() {
        permissions = newPermissions();
        Member primaryElsewhere = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);
        Member target = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        when(sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndVisibleTrue(any(), any()))
                .thenReturn(false);

        assertThat(permissions.canView(primaryElsewhere, target)).isFalse();
    }

    @Test
    void canViewFallsBackToAnExplicitVisibleSharingGrant() {
        permissions = newPermissions();
        Member requester = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        Member target = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        when(sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndVisibleTrue(
                        target.getId(), requester.getId()))
                .thenReturn(true);

        assertThat(permissions.canView(requester, target)).isTrue();
    }

    @Test
    void canViewIsFalseWithoutSelfPrimaryOrAGrant() {
        permissions = newPermissions();
        Member requester = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        Member target = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        when(sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndVisibleTrue(any(), any()))
                .thenReturn(false);

        assertThat(permissions.canView(requester, target)).isFalse();
    }

    // ---- canViewScope ----

    @Test
    void canViewScopeIsTrueForOnesOwnMemberRow() {
        permissions = newPermissions();
        Member self = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);

        assertThat(permissions.canViewScope(self, self, SharingScope.VITALS)).isTrue();
    }

    @Test
    void canViewScopeIsTrueForThePrimaryOfTheSameAccountRegardlessOfScope() {
        permissions = newPermissions();
        UUID accountId = UUID.randomUUID();
        Member primary = memberWithId(UUID.randomUUID(), accountId, MemberRole.PRIMARY);
        Member other = memberWithId(UUID.randomUUID(), accountId, MemberRole.SECONDARY);

        assertThat(permissions.canViewScope(primary, other, SharingScope.BEHAVIOUR)).isTrue();
    }

    @Test
    void canViewScopeIsFalseForAPrimaryOfADifferentAccountWithoutAGrant() {
        permissions = newPermissions();
        Member primaryElsewhere = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.PRIMARY);
        Member target = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        when(sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndScopeAndVisibleTrue(any(), any(), any()))
                .thenReturn(false);

        assertThat(permissions.canViewScope(primaryElsewhere, target, SharingScope.VITALS)).isFalse();
    }

    @Test
    void canViewScopeIsTrueWhenAVisibleGrantExistsForThatExactScope() {
        permissions = newPermissions();
        Member requester = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        Member target = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        when(sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndScopeAndVisibleTrue(
                        target.getId(), requester.getId(), SharingScope.VITALS))
                .thenReturn(true);

        assertThat(permissions.canViewScope(requester, target, SharingScope.VITALS)).isTrue();
    }

    @Test
    void canViewScopeIsFalseWhenTheGrantedScopeDoesNotMatchTheRequestedOne() {
        permissions = newPermissions();
        Member requester = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        Member target = memberWithId(UUID.randomUUID(), UUID.randomUUID(), MemberRole.SECONDARY);
        when(sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndScopeAndVisibleTrue(
                        target.getId(), requester.getId(), SharingScope.BEHAVIOUR))
                .thenReturn(false);

        assertThat(permissions.canViewScope(requester, target, SharingScope.BEHAVIOUR)).isFalse();
    }
}
