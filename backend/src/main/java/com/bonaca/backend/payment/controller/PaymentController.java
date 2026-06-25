package com.bonaca.backend.payment.controller;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.members.exception.ForbiddenMemberAccessException;
import com.bonaca.backend.members.model.Member;
import com.bonaca.backend.members.service.MemberPermissions;
import com.bonaca.backend.payment.dto.PaymentLinkResponse;
import com.bonaca.backend.payment.service.PaymentService;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/subscription")
public class PaymentController {

    private final PaymentService paymentService;
    private final MemberPermissions memberPermissions;

    public PaymentController(PaymentService paymentService, MemberPermissions memberPermissions) {
        this.paymentService = paymentService;
        this.memberPermissions = memberPermissions;
    }

    @PostMapping("/payment-link")
    public ResponseEntity<PaymentLinkResponse> initiatePayment(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims,
            @PathVariable UUID accountId) throws IOException, InterruptedException {
        Member caller = memberPermissions.requireMemberForUser(claims.userId());
        if (!caller.getAccountId().equals(accountId)) {
            throw new ForbiddenMemberAccessException("You don't have access to this account");
        }
        return ResponseEntity.ok(paymentService.initiatePayment(accountId));
    }
}
