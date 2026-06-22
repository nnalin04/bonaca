package com.bonaca.backend.subscriptions.controller;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.subscriptions.dto.SubscriptionResponse;
import com.bonaca.backend.subscriptions.service.SubscriptionService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only stand-in for real payment processing — see
 * docs/TECHNICAL/BACKEND_STATUS_AND_NEXT_STEPS.md §4. No card details, no processor call, just
 * immediately activates the subscription so the rest of the product (trial -> active -> renewal)
 * can be built and demoed without waiting on the unresolved cards-vs-StoreKit decision. Same
 * {@code @Profile("!prod")} pattern already used for {@code LoggingOtpSender} — can never run in
 * a real environment, so there's no risk of this becoming a real "free activation" endpoint.
 */
@RestController
@RequestMapping("/api/v1/accounts/{accountId}/subscription")
@Profile("!prod")
public class MockPaymentController {

    private final SubscriptionService subscriptionService;
    private final MemberPermissions permissions;

    public MockPaymentController(SubscriptionService subscriptionService, MemberPermissions permissions) {
        this.subscriptionService = subscriptionService;
        this.permissions = permissions;
    }

    @PostMapping("/mock-pay")
    public ResponseEntity<SubscriptionResponse> mockPay(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @PathVariable UUID accountId) {
        Member requester = permissions.requireMemberForUser(claims.userId());
        if (!requester.getAccountId().equals(accountId)) {
            throw new ForbiddenMemberAccessException("You don't have access to this account's subscription");
        }
        return ResponseEntity.ok(SubscriptionResponse.from(subscriptionService.activate(accountId, Instant.now())));
    }
}
