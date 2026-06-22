package com.bonaca.backend.subscriptions.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.model.SubscriptionStatus;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionLifecycleSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionService subscriptionService;

    private SubscriptionLifecycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SubscriptionLifecycleScheduler(subscriptionRepository, subscriptionService);
    }

    @Test
    void expireLapsedTrialsTransitionsEveryLapsedTrialAccount() {
        UUID accountOne = UUID.randomUUID();
        UUID accountTwo = UUID.randomUUID();
        when(subscriptionRepository.findByStatusAndTrialEndsAtBefore(eq(SubscriptionStatus.TRIAL), any())).thenReturn(List.of(
                new Subscription(accountOne, SubscriptionStatus.TRIAL, Instant.now().minus(1, ChronoUnit.DAYS)),
                new Subscription(accountTwo, SubscriptionStatus.TRIAL, Instant.now().minus(2, ChronoUnit.DAYS))));

        scheduler.expireLapsedTrials();

        verify(subscriptionService).markExpired(accountOne);
        verify(subscriptionService).markExpired(accountTwo);
    }

    @Test
    void expireLapsedTrialsDoesNothingWhenNoTrialsHaveLapsed() {
        when(subscriptionRepository.findByStatusAndTrialEndsAtBefore(eq(SubscriptionStatus.TRIAL), any())).thenReturn(List.of());

        scheduler.expireLapsedTrials();

        verify(subscriptionService, never()).markExpired(any());
    }
}
