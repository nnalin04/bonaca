package com.bonaca.backend.subscriptions.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

    @Test
    void accessorsReflectConstructorArguments() {
        UUID accountId = UUID.randomUUID();
        Instant trialEndsAt = Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS);
        Subscription subscription = new Subscription(accountId, SubscriptionStatus.TRIAL, trialEndsAt);

        assertThat(subscription.getAccountId()).isEqualTo(accountId);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(subscription.getTrialEndsAt()).isEqualTo(trialEndsAt);
        assertThat(subscription.getRenewedAt()).isNull();
        assertThat(subscription.getCreatedAt()).isNotNull();
        assertThat(subscription.getId()).isNull();
    }

    @Test
    void markActiveSetsStatusActiveAndRecordsRenewedAt() {
        Subscription subscription = new Subscription(UUID.randomUUID(), SubscriptionStatus.TRIAL, Instant.now());
        Instant renewedAt = Instant.now();

        subscription.markActive(renewedAt);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getRenewedAt()).isEqualTo(renewedAt);
    }

    @Test
    void markExpiringSetsStatusExpiring() {
        Subscription subscription = new Subscription(UUID.randomUUID(), SubscriptionStatus.ACTIVE, null);

        subscription.markExpiring();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRING);
    }

    @Test
    void markExpiredSetsStatusExpired() {
        Subscription subscription = new Subscription(UUID.randomUUID(), SubscriptionStatus.TRIAL, Instant.now());

        subscription.markExpired();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    void markCancelledSetsStatusCancelled() {
        Subscription subscription = new Subscription(UUID.randomUUID(), SubscriptionStatus.ACTIVE, null);

        subscription.markCancelled();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }
}
