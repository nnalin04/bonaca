package com.bonaca.backend.members.repository;

import com.bonaca.backend.members.model.SharingGrant;
import com.bonaca.backend.members.model.SharingScope;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharingGrantRepository extends JpaRepository<SharingGrant, UUID> {

    List<SharingGrant> findByGranteeMemberIdAndVisibleTrue(UUID granteeMemberId);

    List<SharingGrant> findByGranterMemberIdInOrGranteeMemberIdIn(List<UUID> granterIds, List<UUID> granteeIds);

    boolean existsByGranterMemberIdAndGranteeMemberIdAndVisibleTrue(UUID granterMemberId, UUID granteeMemberId);

    boolean existsByGranterMemberIdAndGranteeMemberIdAndScopeAndVisibleTrue(
            UUID granterMemberId, UUID granteeMemberId, SharingScope scope);
}
