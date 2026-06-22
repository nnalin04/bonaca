package com.bonaca.backend.members.dto;

public record UpdateMemberRequest(String nickname, Boolean pinned, Boolean hidden) {
}
