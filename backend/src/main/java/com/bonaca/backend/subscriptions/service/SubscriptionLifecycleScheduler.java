package com.bonaca.backend.subscriptions.service;

import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PRD §6.2: a trial that lapses without an activation pauses access. Without a real payment
 * processor (see docs/TECHNICAL/SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md §1), TRIAL -> EXPIRED once
 * trialEndsAt has passed is the only transition that can correctly happen from time passing
 * alone — ACTIVE/EXPIRING transitions need a real renewal attempt this pass doesn't have.
 *
 * <p>Deliberately does not depend on notifications — members already depends on subscriptions
 * (MembersService.completeProfile calls SubscriptionService.startTrial), so subscriptions
 * calling notifications here would close a members -> subscriptions -> notifications -> members
 * cycle (notifications needs members for recipient lookups). See
 * docs/TECHNICAL/NOTIFICATIONS_IMPLEMENTATION_PLAN.md §4 — notifications polls subscriptions
 * state instead, the one-directional shape that avoids it.
 */
@Component
public class SubscriptionLifecycleScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    public SubscriptionLifecycleScheduler(
            SubscriptionRepository subscriptionRepository, SubscriptionService subscriptionService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "${bonaca.subscriptions.lifecycle-cron}")
    public void expireLapsedTrials() {
        for (Subscription subscription :
                subscriptionRepository.findByStatusAndTrialEndsAtBefore(SubscriptionStatus.TRIAL, Instant.now())) {
            subscriptionService.markExpired(subscription.getAccountId());
        }
    }
}
