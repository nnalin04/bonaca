package com.bonaca.backend.members.controller;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.dto.CreateInviteRequest;
import com.bonaca.backend.members.dto.InviteResponse;
import com.bonaca.backend.members.service.InviteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invites")
public class InvitesController {

    private final InviteService inviteService;

    public InvitesController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping
    public ResponseEntity<InviteResponse> createInvite(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @Valid @RequestBody CreateInviteRequest request) {
        return ResponseEntity.ok(inviteService.create(claims.userId(), request.phoneNumber()));
    }

    @GetMapping
    public ResponseEntity<List<InviteResponse>> listInvites(@AuthenticationPrincipal JwtService.AccessTokenClaims claims) {
        return ResponseEntity.ok(inviteService.listForCurrentAccount(claims.userId()));
    }

    @DeleteMapping("/{inviteId}")
    public ResponseEntity<Void> cancelInvite(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @PathVariable UUID inviteId) {
        inviteService.cancelInvite(inviteId, claims.userId());
        return ResponseEntity.noContent().build();
    }
}
