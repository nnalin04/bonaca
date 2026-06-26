package com.bonaca.backend.members.service;

import com.bonaca.backend.members.dto.InviteResponse;
import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.exception.MemberLimitExceededException;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Invite;
import com.bonaca.backend.members.model.InviteStatus;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.repository.InviteRepository;
import com.bonaca.backend.members.repository.MemberRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InviteService {

    private static final int MAX_SECONDARY_MEMBERS = 2;

    private final InviteRepository inviteRepository;
    private final MemberRepository memberRepository;
    private final MemberPermissions permissions;

    public InviteService(InviteRepository inviteRepository, MemberRepository memberRepository, MemberPermissions permissions) {
        this.inviteRepository = inviteRepository;
        this.memberRepository = memberRepository;
        this.permissions = permissions;
    }

    /** Every invite offers the Secondary role — it's the only assignable role besides Primary, which is taken at account creation. */
    @Transactional
    public InviteResponse create(UUID userId, String phoneNumber) {
        Member me = permissions.requireMemberForUser(userId);
        permissions.requireAccountOwner(me, me.getAccountId());

        Invite existing = inviteRepository
                .findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(phoneNumber, InviteStatus.PENDING)
                .orElse(null);
        if (existing != null && existing.getInviterAccountId().equals(me.getAccountId())) {
            return InviteResponse.from(existing);
        }

        long secondaryMembers = memberRepository.findByAccountId(me.getAccountId()).stream()
                .filter(m -> m.getRole() == MemberRole.SECONDARY)
                .count();
        long pendingSecondaryInvites = inviteRepository.findByInviterAccountId(me.getAccountId()).stream()
                .filter(i -> i.getStatus() == InviteStatus.PENDING)
                .count();
        if (secondaryMembers + pendingSecondaryInvites >= MAX_SECONDARY_MEMBERS) {
            throw new MemberLimitExceededException(
                    "This account already has the maximum of " + MAX_SECONDARY_MEMBERS + " Secondary Members (including pending invites)");
        }

        Invite invite = inviteRepository.save(new Invite(me.getAccountId(), phoneNumber, MemberRole.SECONDARY));
        return InviteResponse.from(invite);
    }

    @Transactional
    public void cancelInvite(UUID inviteId, UUID callerId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new MemberNotFoundException("Invite not found: " + inviteId));
        Member caller = permissions.requireMemberForUser(callerId);
        if (!invite.getInviterAccountId().equals(caller.getAccountId()) || caller.getRole() != MemberRole.PRIMARY) {
            throw new ForbiddenMemberAccessException("Only primary members can cancel invites");
        }
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new MemberLimitExceededException("Cannot cancel a non-pending invite");
        }
        invite.cancel();
        inviteRepository.save(invite);
    }

    public List<InviteResponse> listForCurrentAccount(UUID userId) {
        Member me = permissions.requireMemberForUser(userId);
        permissions.requireAccountOwner(me, me.getAccountId());
        return inviteRepository.findByInviterAccountId(me.getAccountId()).stream()
                .map(InviteResponse::from)
                .toList();
    }
}
