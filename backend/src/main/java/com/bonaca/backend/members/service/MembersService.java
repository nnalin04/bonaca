package com.bonaca.backend.members.service;

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
import com.bonaca.backend.members.model.SharingScope;
import com.bonaca.backend.members.repository.AccountRepository;
import com.bonaca.backend.members.repository.InviteRepository;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.repository.SharingGrantRepository;
import com.bonaca.backend.subscriptions.service.SubscriptionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembersService {

    private static final int MAX_SECONDARY_MEMBERS = 2;

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final InviteRepository inviteRepository;
    private final SharingGrantRepository sharingGrantRepository;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final MemberPermissions permissions;

    public MembersService(
            AccountRepository accountRepository,
            MemberRepository memberRepository,
            InviteRepository inviteRepository,
            SharingGrantRepository sharingGrantRepository,
            SubscriptionService subscriptionService,
            UserRepository userRepository,
            MemberPermissions permissions) {
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.inviteRepository = inviteRepository;
        this.sharingGrantRepository = sharingGrantRepository;
        this.subscriptionService = subscriptionService;
        this.userRepository = userRepository;
        this.permissions = permissions;
    }

    /**
     * Creates the Account+primary Member on first completion, or — if a pending Invite exists
     * for this phone number — attaches the user as that invite's role to the inviter's account
     * instead, with the default SharingGrants described in the members & sharing plan.
     */
    @Transactional
    public MemberResponse completeProfile(UUID userId, CompleteProfileRequest request) {
        if (memberRepository.findByUserId(userId).isPresent()) {
            throw new ForbiddenMemberAccessException("Profile is already complete");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new MemberNotFoundException("User no longer exists"));

        var pendingInvite =
                inviteRepository.findFirstByPhoneNumberAndStatusOrderByCreatedAtAsc(user.getPhoneNumber(), InviteStatus.PENDING);

        Member member;
        if (pendingInvite.isPresent()) {
            Invite invite = pendingInvite.get();
            List<Member> existingAccountMembers = memberRepository.findByAccountId(invite.getInviterAccountId());

            long existingSecondaries =
                    existingAccountMembers.stream().filter(m -> m.getRole() == MemberRole.SECONDARY).count();
            if (existingSecondaries >= MAX_SECONDARY_MEMBERS) {
                throw new MemberLimitExceededException(
                        "This account already has the maximum of " + MAX_SECONDARY_MEMBERS + " Secondary Members");
            }

            member = memberRepository.save(new Member(
                    invite.getInviterAccountId(),
                    userId,
                    invite.getRoleOffered(),
                    request.name(),
                    request.gender(),
                    request.dob(),
                    request.heightCm(),
                    request.weightKg()));

            invite.markAccepted(member.getId());
            inviteRepository.save(invite);
            createDefaultGrants(member, existingAccountMembers);
        } else {
            Account account = accountRepository.save(new Account());
            member = memberRepository.save(new Member(
                    account.getId(),
                    userId,
                    MemberRole.PRIMARY,
                    request.name(),
                    request.gender(),
                    request.dob(),
                    request.heightCm(),
                    request.weightKg()));
            account.setOwnerMemberId(member.getId());
            accountRepository.save(account);
            subscriptionService.startTrial(account.getId());
        }

        user.markProfileCompleted();
        userRepository.save(user);

        return MemberResponse.from(member, true);
    }

    /**
     * Default grants on accept: all access is enabled by default (docs/PRD.pdf §11.1) — the new
     * Secondary Member is granted all scopes on the Primary's (data owner's) data immediately,
     * narrowed later via instant-apply Edit Permissions, not opted in to piece by piece.
     */
    private void createDefaultGrants(Member newMember, List<Member> existingAccountMembers) {
        existingAccountMembers.stream()
                .filter(m -> m.getRole() == MemberRole.PRIMARY)
                .findFirst()
                .ifPresent(primary -> grantAllScopes(primary.getId(), newMember.getId()));
    }

    private void grantAllScopes(UUID granterMemberId, UUID granteeMemberId) {
        for (SharingScope scope : SharingScope.values()) {
            sharingGrantRepository.save(new SharingGrant(granterMemberId, granteeMemberId, scope, true));
        }
    }

    public List<MemberResponse> listVisibleMembers(UUID userId) {
        Member me = permissions.requireMemberForUser(userId);

        List<Member> visible;
        if (me.getRole() == MemberRole.PRIMARY) {
            visible = memberRepository.findByAccountId(me.getAccountId());
        } else {
            List<UUID> visibleGranterIds = sharingGrantRepository.findByGranteeMemberIdAndVisibleTrue(me.getId()).stream()
                    .map(SharingGrant::getGranterMemberId)
                    .distinct()
                    .toList();
            visible = new ArrayList<>(memberRepository.findByIdIn(visibleGranterIds));
            if (visible.stream().noneMatch(m -> m.getId().equals(me.getId()))) {
                visible.add(me);
            }
        }

        return visible.stream()
                .sorted(Comparator.comparing(Member::isPinned).reversed().thenComparing(Member::getCreatedAt))
                .map(m -> MemberResponse.from(m, m.getId().equals(me.getId())))
                .collect(Collectors.toList());
    }

    public MemberResponse getMember(UUID userId, UUID memberId) {
        Member me = permissions.requireMemberForUser(userId);
        Member target = memberRepository.findById(memberId).orElseThrow(() -> new MemberNotFoundException("Member not found"));
        if (!permissions.canView(me, target)) {
            throw new ForbiddenMemberAccessException("You don't have access to this member");
        }
        return MemberResponse.from(target, target.getId().equals(me.getId()));
    }

    @Transactional
    public MemberResponse updateMember(UUID userId, UUID memberId, UpdateMemberRequest request) {
        Member me = permissions.requireMemberForUser(userId);
        Member target = memberRepository.findById(memberId).orElseThrow(() -> new MemberNotFoundException("Member not found"));
        permissions.requireAccountOwner(me, target.getAccountId());

        if (request.nickname() != null) {
            target.setNickname(request.nickname());
        }
        if (request.pinned() != null) {
            target.setPinned(request.pinned());
        }
        if (request.hidden() != null) {
            target.setHidden(request.hidden());
        }
        Member saved = memberRepository.save(target);
        return MemberResponse.from(saved, saved.getId().equals(me.getId()));
    }
}
