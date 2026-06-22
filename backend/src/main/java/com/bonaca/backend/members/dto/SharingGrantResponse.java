package com.bonaca.backend.members.dto;

import com.bonaca.backend.members.model.SharingGrant;
import java.util.UUID;

public record SharingGrantResponse(UUID id, UUID granterMemberId, UUID granteeMemberId, String scope, boolean visible) {

    public static SharingGrantResponse from(SharingGrant grant) {
        return new SharingGrantResponse(
                grant.getId(),
                grant.getGranterMemberId(),
                grant.getGranteeMemberId(),
                grant.getScope().name().toLowerCase(),
                grant.isVisible());
    }
}
