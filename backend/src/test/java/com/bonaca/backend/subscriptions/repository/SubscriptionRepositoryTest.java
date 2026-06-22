package com.bonaca.backend.subscriptions.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Schema contract: account_id is UNIQUE on subscriptions — an account has exactly one. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void savedSubscriptionCanBeFoundById() {
        Subscription subscription = subscriptionRepository.saveAndFlush(
                new Subscription(UUID.randomUUID(), SubscriptionStatus.TRIAL, Instant.now().plus(7, ChronoUnit.DAYS)));

        assertThat(subscriptionRepository.findById(subscription.getId())).isPresent();
    }

    @Test
    void accountIdMustBeUnique() {
        UUID accountId = UUID.randomUUID();
        subscriptionRepository.saveAndFlush(
                new Subscription(accountId, SubscriptionStatus.TRIAL, Instant.now().plus(7, ChronoUnit.DAYS)));

        assertThatThrownBy(() -> subscriptionRepository.saveAndFlush(
                        new Subscription(accountId, SubscriptionStatus.ACTIVE, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByAccountIdReturnsTheMatchingSubscription() {
        UUID accountId = UUID.randomUUID();
        subscriptionRepository.saveAndFlush(
                new Subscription(accountId, SubscriptionStatus.TRIAL, Instant.now().plus(7, ChronoUnit.DAYS)));

        assertThat(subscriptionRepository.findByAccountId(accountId)).isPresent();
    }

    @Test
    void findByStatusAndTrialEndsAtBeforeReturnsOnlyLapsedTrials() {
        Instant now = Instant.now();
        Subscription lapsedTrial =
                subscriptionRepository.saveAndFlush(new Subscription(UUID.randomUUID(), SubscriptionStatus.TRIAL, now.minus(1, ChronoUnit.DAYS)));
        subscriptionRepository.saveAndFlush(
                new Subscription(UUID.randomUUID(), SubscriptionStatus.TRIAL, now.plus(1, ChronoUnit.DAYS))); // not yet lapsed
        subscriptionRepository.saveAndFlush(
                new Subscription(UUID.randomUUID(), SubscriptionStatus.ACTIVE, null)); // not a trial at all

        var result = subscriptionRepository.findByStatusAndTrialEndsAtBefore(SubscriptionStatus.TRIAL, now);

        assertThat(result).extracting(Subscription::getId).containsExactly(lapsedTrial.getId());
    }

    @Test
    void findByStatusReturnsOnlySubscriptionsInThatStatus() {
        Subscription expired = subscriptionRepository.saveAndFlush(new Subscription(UUID.randomUUID(), SubscriptionStatus.EXPIRED, null));
        subscriptionRepository.saveAndFlush(new Subscription(UUID.randomUUID(), SubscriptionStatus.TRIAL, Instant.now()));

        var result = subscriptionRepository.findByStatus(SubscriptionStatus.EXPIRED);

        assertThat(result).extracting(Subscription::getId).containsExactly(expired.getId());
    }
}
