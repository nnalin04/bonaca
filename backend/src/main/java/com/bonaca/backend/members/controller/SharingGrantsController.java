package com.bonaca.backend.members.controller;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.dto.SharingGrantResponse;
import com.bonaca.backend.members.dto.UpdateSharingGrantRequest;
import com.bonaca.backend.members.service.SharingGrantService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sharing-grants")
public class SharingGrantsController {

    private final SharingGrantService sharingGrantService;

    public SharingGrantsController(SharingGrantService sharingGrantService) {
        this.sharingGrantService = sharingGrantService;
    }

    @GetMapping
    public ResponseEntity<List<SharingGrantResponse>> listGrants(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @RequestParam UUID accountId) {
        return ResponseEntity.ok(sharingGrantService.listForAccount(claims.userId(), accountId));
    }

    @PatchMapping("/{grantId}")
    public ResponseEntity<SharingGrantResponse> updateGrant(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims,
            @PathVariable UUID grantId,
            @Valid @RequestBody UpdateSharingGrantRequest request) {
        return ResponseEntity.ok(sharingGrantService.updateVisibility(claims.userId(), grantId, request.visible()));
    }
}
