package com.bonaca.backend.subscriptions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bonaca.backend.subscriptions.exception.SubscriptionNotFoundException;
import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Contract from SubscriptionService's class Javadoc + docs/TECHNICAL/SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md
 * §3: startTrial creates a 7-day TRIAL (PRD §6.1); isActive treats TRIAL/ACTIVE/EXPIRING as
 * active and EXPIRED/CANCELLED/no-row-at-all as inactive (§3.1).
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptionRepository);
    }

    @Test
    void startTrialCreatesATrialSubscriptionEndingSevenDaysFromNow() {
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        Subscription result = service.startTrial(ACCOUNT_ID);

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(result.getTrialEndsAt()).isCloseTo(before.plus(7, ChronoUnit.DAYS), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void activateMarksAnExistingSubscriptionActiveWithTheGivenRenewalTime() {
        Subscription existing = new Subscription(ACCOUNT_ID, SubscriptionStatus.TRIAL, Instant.now());
        when(subscriptionRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(existing)).thenReturn(existing);
        Instant renewedAt = Instant.now();

        Subscription result = service.activate(ACCOUNT_ID, renewedAt);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.getRenewedAt()).isEqualTo(renewedAt);
    }

    @Test
    void activateThrowsWhenNoSubscriptionExistsForTheAccount() {
        when(subscriptionRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(ACCOUNT_ID, Instant.now())).isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void markExpiringTransitionsAnExistingSubscription() {
        Subscription existing = new Subscription(ACCOUNT_ID, SubscriptionStatus.ACTIVE, null);
        when(subscriptionRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(existing)).thenReturn(existing);

        Subscription result = service.markExpiring(ACCOUNT_ID);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.EXPIRING);
    }

    @Test
    void markExpiredTransitionsAnExistingSubscription() {
        Subscription existing = new Subscription(ACCOUNT_ID, SubscriptionStatus.TRIAL, Instant.now());
        when(subscriptionRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(existing)).thenReturn(existing);

        Subscription result = service.markExpired(ACCOUNT_ID);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    void cancelTransitionsAnExistingSubscription() {
        Subscription existing = new Subscription(ACCOUNT_ID, SubscriptionStatus.ACTIVE, null);
        when(subscriptionRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(existing)).thenReturn(existing);

        Subscription result = service.cancel(ACCOUNT_ID);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void isActiveIsTrueForTrialActiveAndExpiring() {
        for (SubscriptionStatus status : new SubscriptionStatus[] {SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRING}) {
            when(subscriptionRepository.findByAccountId(ACCOUNT_ID))
                    .thenReturn(Optional.of(new Subscription(ACCOUNT_ID, status, null)));

            assertThat(service.isActive(ACCOUNT_ID)).as("status %s should be active", status).isTrue();
        }
    }

    @Test
    void isActiveIsFalseForExpiredAndCancelled() {
        for (SubscriptionStatus status : new SubscriptionStatus[] {SubscriptionStatus.EXPIRED, SubscriptionStatus.CANCELLED}) {
            when(subscriptionRepository.findByAccountId(ACCOUNT_ID))
                    .thenReturn(Optional.of(new Subscription(ACCOUNT_ID, status, null)));

            assertThat(service.isActive(ACCOUNT_ID)).as("status %s should be inactive", status).isFalse();
        }
    }

    @Test
    void isActiveIsFalseWhenNoSubscriptionRowExistsForTheAccount() {
        when(subscriptionRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThat(service.isActive(ACCOUNT_ID)).isFalse();
    }

    @Test
    void getForAccountReturnsThePersistedSubscription() {
        Subscription existing = new Subscription(ACCOUNT_ID, SubscriptionStatus.TRIAL, Instant.now());
        when(subscriptionRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(existing));

        assertThat(service.getForAccount(ACCOUNT_ID)).isSameAs(existing);
    }

    @Test
    void getForAccountThrowsWhenNoSubscriptionExists() {
        when(subscriptionRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForAccount(ACCOUNT_ID)).isInstanceOf(SubscriptionNotFoundException.class);
    }
}
