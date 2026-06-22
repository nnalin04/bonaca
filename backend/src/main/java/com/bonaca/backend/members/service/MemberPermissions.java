package com.bonaca.backend.members.service;

import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.MemberRole;
import com.bonaca.backend.members.model.SharingScope;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.repository.SharingGrantRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The real authorization layer for the members/sharing model (Spring Security
 * method-level checks per docs/TECHNICAL/BACKEND_CUSTOM_IMPLEMENTATION.md §4) —
 * not Postgres RLS. Plain explicit checks rather than @PreAuthorize SpEL, since
 * the JWT principal (JwtService.AccessTokenClaims) is a record and SpEL property
 * access on records isn't reliable enough to depend on here.
 */
@Component
public class MemberPermissions {

    private final MemberRepository memberRepository;
    private final SharingGrantRepository sharingGrantRepository;

    public MemberPermissions(MemberRepository memberRepository, SharingGrantRepository sharingGrantRepository) {
        this.memberRepository = memberRepository;
        this.sharingGrantRepository = sharingGrantRepository;
    }

    public Member requireMemberForUser(UUID userId) {
        return memberRepository
                .findByUserId(userId)
                .orElseThrow(() -> new MemberNotFoundException("Complete your profile first"));
    }

    public void requireAccountOwner(Member requester, UUID accountId) {
        if (requester.getRole() != MemberRole.PRIMARY || !requester.getAccountId().equals(accountId)) {
            throw new ForbiddenMemberAccessException("Only the primary member can manage this");
        }
    }

    public boolean canView(Member requester, Member target) {
        if (requester.getId().equals(target.getId())) {
            return true;
        }
        if (requester.getRole() == MemberRole.PRIMARY && requester.getAccountId().equals(target.getAccountId())) {
            return true;
        }
        return sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndVisibleTrue(
                target.getId(), requester.getId());
    }

    /**
     * Scope-level visibility, for callers (e.g. metrics) that need to know whether a specific
     * permission category is granted, not just "is this member visible at all" — PRD §11.2's
     * actual Vitals/Activity/Behaviour categories aren't interchangeable.
     */
    public boolean canViewScope(Member requester, Member target, SharingScope scope) {
        if (requester.getId().equals(target.getId())) {
            return true;
        }
        if (requester.getRole() == MemberRole.PRIMARY && requester.getAccountId().equals(target.getAccountId())) {
            return true;
        }
        return sharingGrantRepository.existsByGranterMemberIdAndGranteeMemberIdAndScopeAndVisibleTrue(
                target.getId(), requester.getId(), scope);
    }
}
