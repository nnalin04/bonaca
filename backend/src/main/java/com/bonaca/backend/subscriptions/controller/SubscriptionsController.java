package com.bonaca.backend.subscriptions.controller;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.subscriptions.dto.SubscriptionResponse;
import com.bonaca.backend.subscriptions.service.SubscriptionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Status query only — see docs/TECHNICAL/SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md §6. No write
 * endpoint; SubscriptionService's transition methods are internal, called by MembersService
 * (trial start) and, eventually, a real payment integration.
 */
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/subscription")
public class SubscriptionsController {

    private final SubscriptionService subscriptionService;
    private final MemberPermissions permissions;

    public SubscriptionsController(SubscriptionService subscriptionService, MemberPermissions permissions) {
        this.subscriptionService = subscriptionService;
        this.permissions = permissions;
    }

    @GetMapping
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @PathVariable UUID accountId) {
        Member requester = permissions.requireMemberForUser(claims.userId());
        if (!requester.getAccountId().equals(accountId)) {
            throw new ForbiddenMemberAccessException("You don't have access to this account's subscription");
        }
        return ResponseEntity.ok(SubscriptionResponse.from(subscriptionService.getForAccount(accountId)));
    }
}
