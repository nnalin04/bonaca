package com.bonaca.backend.members.dto;

import com.bonaca.backend.members.model.Invite;
import java.util.UUID;

public record InviteResponse(UUID id, String phoneNumber, String roleOffered, String status) {

    public static InviteResponse from(Invite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getPhoneNumber(),
                invite.getRoleOffered().name().toLowerCase(),
                invite.getStatus().name().toLowerCase());
    }
}
