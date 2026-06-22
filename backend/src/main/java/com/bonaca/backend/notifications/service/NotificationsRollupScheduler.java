package com.bonaca.backend.notifications.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs after metrics' (02:00) and subscriptions' (03:00) own nightly jobs — see
 * docs/TECHNICAL/NOTIFICATIONS_IMPLEMENTATION_PLAN.md §3. No new infrastructure, same
 * Spring-scheduler approach as MetricsRollupScheduler/SubscriptionLifecycleScheduler.
 */
@Component
public class NotificationsRollupScheduler {

    private final NotificationGenerationService notificationGenerationService;

    public NotificationsRollupScheduler(NotificationGenerationService notificationGenerationService) {
        this.notificationGenerationService = notificationGenerationService;
    }

    @Scheduled(cron = "${bonaca.notifications.rollup-cron}")
    public void runNightlyRollup() {
        notificationGenerationService.generateMetricAnomalyNotifications();
        notificationGenerationService.generateLapsedSubscriptionNotifications();
    }
}
