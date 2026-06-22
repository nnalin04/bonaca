package com.bonaca.backend.subscriptions.dto;

import com.bonaca.backend.subscriptions.model.Subscription;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(UUID id, UUID accountId, String status, Instant trialEndsAt, Instant renewedAt) {

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getAccountId(),
                subscription.getStatus().name().toLowerCase(),
                subscription.getTrialEndsAt(),
                subscription.getRenewedAt());
    }
}
