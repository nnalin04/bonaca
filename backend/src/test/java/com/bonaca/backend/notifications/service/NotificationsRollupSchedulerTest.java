package com.bonaca.backend.notifications.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationsRollupSchedulerTest {

    @Mock
    private NotificationGenerationService notificationGenerationService;

    @Test
    void nightlyRollupGeneratesMetricAnomalyAndLapsedSubscriptionNotifications() {
        NotificationsRollupScheduler scheduler = new NotificationsRollupScheduler(notificationGenerationService);

        scheduler.runNightlyRollup();

        verify(notificationGenerationService).generateMetricAnomalyNotifications();
        verify(notificationGenerationService).generateLapsedSubscriptionNotifications();
    }
}
