package com.bonaca.backend.members.controller;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.dto.CompleteProfileRequest;
import com.bonaca.backend.members.dto.MemberResponse;
import com.bonaca.backend.members.dto.UpdateMemberRequest;
import com.bonaca.backend.members.service.MembersService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MembersController {

    private final MembersService membersService;

    public MembersController(MembersService membersService) {
        this.membersService = membersService;
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<MemberResponse> completeProfile(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @Valid @RequestBody CompleteProfileRequest request) {
        return ResponseEntity.ok(membersService.completeProfile(claims.userId(), request));
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> listMembers(@AuthenticationPrincipal JwtService.AccessTokenClaims claims) {
        return ResponseEntity.ok(membersService.listVisibleMembers(claims.userId()));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMember(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @PathVariable UUID memberId) {
        return ResponseEntity.ok(membersService.getMember(claims.userId(), memberId));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims,
            @PathVariable UUID memberId,
            @RequestBody UpdateMemberRequest request) {
        return ResponseEntity.ok(membersService.updateMember(claims.userId(), memberId, request));
    }
}
