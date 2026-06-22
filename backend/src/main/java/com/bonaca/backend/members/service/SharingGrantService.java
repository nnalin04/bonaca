package com.bonaca.backend.members.service;

import com.bonaca.backend.members.dto.SharingGrantResponse;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.model.SharingGrant;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.repository.SharingGrantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharingGrantService {

    private final SharingGrantRepository sharingGrantRepository;
    private final MemberRepository memberRepository;
    private final MemberPermissions permissions;

    public SharingGrantService(
            SharingGrantRepository sharingGrantRepository, MemberRepository memberRepository, MemberPermissions permissions) {
        this.sharingGrantRepository = sharingGrantRepository;
        this.memberRepository = memberRepository;
        this.permissions = permissions;
    }

    public List<SharingGrantResponse> listForAccount(UUID userId, UUID accountId) {
        Member me = permissions.requireMemberForUser(userId);
        permissions.requireAccountOwner(me, accountId);

        List<UUID> accountMemberIds =
                memberRepository.findByAccountId(accountId).stream().map(Member::getId).toList();

        return sharingGrantRepository.findByGranterMemberIdInOrGranteeMemberIdIn(accountMemberIds, accountMemberIds).stream()
                .map(SharingGrantResponse::from)
                .toList();
    }

    @Transactional
    public SharingGrantResponse updateVisibility(UUID userId, UUID grantId, boolean visible) {
        Member me = permissions.requireMemberForUser(userId);
        SharingGrant grant =
                sharingGrantRepository.findById(grantId).orElseThrow(() -> new MemberNotFoundException("Sharing grant not found"));

        Member granter = memberRepository
                .findById(grant.getGranterMemberId())
                .orElseThrow(() -> new MemberNotFoundException("Member not found"));
        permissions.requireAccountOwner(me, granter.getAccountId());

        grant.setVisible(visible);
        return SharingGrantResponse.from(sharingGrantRepository.save(grant));
    }
}
