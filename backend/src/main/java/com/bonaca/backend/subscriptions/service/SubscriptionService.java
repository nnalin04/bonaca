package com.bonaca.backend.subscriptions.service;

import com.bonaca.backend.subscriptions.exception.SubscriptionNotFoundException;
import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Billing state only — see docs/TECHNICAL/SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md §1. No real
 * payment processor exists yet (CLAUDE.md explicitly blocks that work pending an unresolved
 * cards-vs-Apple-StoreKit decision), so {@link #activate}, {@link #markExpiring}, and
 * {@link #cancel} are entry points a future payment integration calls — nothing in this codebase
 * calls them yet except tests.
 */
@Service
public class SubscriptionService {

    private static final int TRIAL_DAYS = 7;

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    /** Called by MembersService.completeProfile when a new Account is created — see PRD §6.1. */
    @Transactional
    public Subscription startTrial(UUID accountId) {
        Subscription subscription =
                new Subscription(accountId, SubscriptionStatus.TRIAL, Instant.now().plus(TRIAL_DAYS, ChronoUnit.DAYS));
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription activate(UUID accountId, Instant renewedAt) {
        Subscription subscription = requireSubscription(accountId);
        subscription.markActive(renewedAt);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription markExpiring(UUID accountId) {
        Subscription subscription = requireSubscription(accountId);
        subscription.markExpiring();
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription markExpired(UUID accountId) {
        Subscription subscription = requireSubscription(accountId);
        subscription.markExpired();
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription cancel(UUID accountId) {
        Subscription subscription = requireSubscription(accountId);
        subscription.markCancelled();
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription recordRenewal(UUID accountId, Instant nextBillingAt) {
        Subscription subscription = requireSubscription(accountId);
        subscription.markActive(Instant.now());
        subscription.setNextBillingAt(nextBillingAt);
        return subscriptionRepository.save(subscription);
    }

    /**
     * PRD §12: a lapsed subscription pauses wearable sync and sharing, but TRIAL/ACTIVE/EXPIRING
     * all still count as active — EXPIRING is a failed-renewal grace period, not a denial yet
     * (see the plan doc §3.1). An account with no subscription row at all (not yet onboarded)
     * is inactive, not an error.
     */
    public boolean isActive(UUID accountId) {
        return subscriptionRepository
                .findByAccountId(accountId)
                .map(subscription -> subscription.getStatus() == SubscriptionStatus.TRIAL
                        || subscription.getStatus() == SubscriptionStatus.ACTIVE
                        || subscription.getStatus() == SubscriptionStatus.EXPIRING)
                .orElse(false);
    }

    public Subscription getForAccount(UUID accountId) {
        return requireSubscription(accountId);
    }

    private Subscription requireSubscription(UUID accountId) {
        return subscriptionRepository
                .findByAccountId(accountId)
                .orElseThrow(() -> new SubscriptionNotFoundException("No subscription exists for this account"));
    }
}
