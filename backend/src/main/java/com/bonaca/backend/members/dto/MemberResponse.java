package com.bonaca.backend.members.dto;

import com.bonaca.backend.members.model.Member;
import java.time.LocalDate;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID accountId,
        String role,
        String name,
        String nickname,
        boolean pinned,
        boolean hidden,
        String statusMessage,
        String gender,
        LocalDate dob,
        Integer heightCm,
        Integer weightKg,
        boolean self) {

    /** self is true when this response represents the requesting user's own Member row. */
    public static MemberResponse from(Member member, boolean self) {
        return new MemberResponse(
                member.getId(),
                member.getAccountId(),
                member.getRole().name().toLowerCase(),
                member.getName(),
                member.getNickname(),
                member.isPinned(),
                member.isHidden(),
                member.getStatusMessage(),
                member.getGender(),
                member.getDob(),
                member.getHeightCm(),
                member.getWeightKg(),
                self);
    }
}
