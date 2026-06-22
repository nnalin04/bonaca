package com.bonaca.backend.notifications.controller;

import com.bonaca.backend.auth.service.JwtService;
import com.bonaca.backend.notifications.dto.NotificationResponse;
import com.bonaca.backend.notifications.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationsController {

    private final NotificationService notificationService;

    public NotificationsController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/api/v1/members/{memberId}/notifications")
    public ResponseEntity<List<NotificationResponse>> listNotifications(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @PathVariable UUID memberId) {
        return ResponseEntity.ok(notificationService.listForMember(claims.userId(), memberId).stream()
                .map(NotificationResponse::from)
                .toList());
    }

    @PatchMapping("/api/v1/notifications/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @PathVariable UUID notificationId) {
        return ResponseEntity.ok(NotificationResponse.from(notificationService.markRead(claims.userId(), notificationId)));
    }

    @PostMapping("/api/v1/members/{memberId}/notifications/payment-request")
    public ResponseEntity<NotificationResponse> requestPayment(
            @AuthenticationPrincipal JwtService.AccessTokenClaims claims, @PathVariable UUID memberId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NotificationResponse.from(notificationService.requestPayment(claims.userId(), memberId)));
    }
}
