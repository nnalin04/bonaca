package com.bonaca.backend.members.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bonaca.backend.members.dto.SharingGrantResponse;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.model.SharingGrant;
import com.bonaca.backend.members.model.SharingScope;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.repository.SharingGrantRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharingGrantServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private SharingGrantRepository sharingGrantRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPermissions permissions;

    private SharingGrantService sharingGrantService;

    @BeforeEach
    void setUp() {
        sharingGrantService = new SharingGrantService(sharingGrantRepository, memberRepository, permissions);
    }

    private static Member member(UUID accountId) {
        Member m = new Member(accountId, UUID.randomUUID(), MemberRole.PRIMARY, "Name", null, null, null, null);
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
    void listForAccountReturnsGrantsInvolvingTheAccountsMembers() {
        UUID accountId = UUID.randomUUID();
        Member me = member(accountId);
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        when(memberRepository.findByAccountId(accountId)).thenReturn(List.of(me));
        SharingGrant grant = new SharingGrant(me.getId(), UUID.randomUUID(), SharingScope.VITALS, true);
        when(sharingGrantRepository.findByGranterMemberIdInOrGranteeMemberIdIn(List.of(me.getId()), List.of(me.getId())))
                .thenReturn(List.of(grant));

        List<SharingGrantResponse> result = sharingGrantService.listForAccount(USER_ID, accountId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).scope()).isEqualTo("vitals");
        verify(permissions).requireAccountOwner(me, accountId);
    }

    @Test
    void updateVisibilityFlipsAndSavesTheGrant() {
        Member me = member(UUID.randomUUID());
        Member granter = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        SharingGrant grant = new SharingGrant(granter.getId(), UUID.randomUUID(), SharingScope.ACTIVITY, false);
        UUID grantId = UUID.randomUUID();
        when(sharingGrantRepository.findById(grantId)).thenReturn(Optional.of(grant));
        when(memberRepository.findById(granter.getId())).thenReturn(Optional.of(granter));
        when(sharingGrantRepository.save(grant)).thenReturn(grant);

        SharingGrantResponse response = sharingGrantService.updateVisibility(USER_ID, grantId, true);

        assertThat(response.visible()).isTrue();
        assertThat(grant.isVisible()).isTrue();
        verify(permissions).requireAccountOwner(me, granter.getAccountId());
    }

    @Test
    void updateVisibilityThrowsWhenGrantDoesNotExist() {
        Member me = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        UUID grantId = UUID.randomUUID();
        when(sharingGrantRepository.findById(grantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingGrantService.updateVisibility(USER_ID, grantId, true))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void updateVisibilityThrowsWhenTheGranterMemberNoLongerExists() {
        Member me = member(UUID.randomUUID());
        when(permissions.requireMemberForUser(USER_ID)).thenReturn(me);
        SharingGrant grant = new SharingGrant(UUID.randomUUID(), UUID.randomUUID(), SharingScope.BEHAVIOUR, true);
        UUID grantId = UUID.randomUUID();
        when(sharingGrantRepository.findById(grantId)).thenReturn(Optional.of(grant));
        when(memberRepository.findById(grant.getGranterMemberId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingGrantService.updateVisibility(USER_ID, grantId, true))
                .isInstanceOf(MemberNotFoundException.class);
    }
}
