package com.bonaca.backend.wearable.controller;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.wearable.dto.ConnectUrlResponse;
import com.bonaca.backend.wearable.dto.WearableConnectionResponse;
import com.bonaca.backend.wearable.model.WearableConnection;
import com.bonaca.backend.wearable.service.WearableService;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/{memberId}/wearable")
public class WearableController {

    private final WearableService wearableService;
    private final MemberPermissions memberPermissions;
    private final MemberRepository memberRepository;

    public WearableController(
            WearableService wearableService,
            MemberPermissions memberPermissions,
            MemberRepository memberRepository) {
        this.wearableService = wearableService;
        this.memberPermissions = memberPermissions;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/connect")
    public ResponseEntity<ConnectUrlResponse> connect(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims,
            @PathVariable UUID memberId) throws IOException, InterruptedException {
        Member caller = memberPermissions.requireMemberForUser(claims.userId());
        if (!caller.getId().equals(memberId)) {
            throw new ForbiddenMemberAccessException("You can only connect a wearable for your own account");
        }
        WearableConnection connection = wearableService.getOrCreateConnection(memberId);
        return ResponseEntity.ok(new ConnectUrlResponse(connection.getConnectUrl(), connection.getSpikeUserId()));
    }

    @GetMapping("/connection")
    public ResponseEntity<WearableConnectionResponse> getConnection(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims,
            @PathVariable UUID memberId) {
        Member caller = memberPermissions.requireMemberForUser(claims.userId());
        Member target = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found: " + memberId));
        if (!caller.getId().equals(memberId) && !memberPermissions.canView(caller, target)) {
            throw new ForbiddenMemberAccessException("Access denied");
        }
        Optional<WearableConnection> connection = wearableService.getConnection(memberId);
        return connection.map(c -> ResponseEntity.ok(WearableConnectionResponse.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/connection")
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims,
            @PathVariable UUID memberId) throws IOException, InterruptedException {
        Member caller = memberPermissions.requireMemberForUser(claims.userId());
        if (!caller.getId().equals(memberId)) {
            throw new ForbiddenMemberAccessException("You can only disconnect your own wearable");
        }
        wearableService.disconnect(memberId);
        return ResponseEntity.noContent().build();
    }
}
