package com.bonaca.backend.subscriptions.repository;

import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByAccountId(UUID accountId);

    /** Used by SubscriptionLifecycleScheduler's nightly job to find lapsed trials. */
    List<Subscription> findByStatusAndTrialEndsAtBefore(SubscriptionStatus status, Instant cutoff);

    /** Used by notifications.service.NotificationGenerationService's nightly lapsed-subscription scan. */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /** Used by RazorpayWebhookController to resolve an account from a webhook payload. */
    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);
}
