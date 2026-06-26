package com.bonaca.backend.payment.service;

import com.bonaca.backend.payment.config.RazorpayProperties;
import com.bonaca.backend.payment.dto.PaymentLinkResponse;
import com.bonaca.backend.payment.model.PaymentEvent;
import com.bonaca.backend.payment.repository.PaymentEventRepository;
import com.bonaca.backend.subscriptions.exception.SubscriptionNotFoundException;
import com.bonaca.backend.subscriptions.model.Subscription;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import com.bonaca.backend.subscriptions.service.SubscriptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties(RazorpayProperties.class)
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final RazorpayClient razorpayClient;
    private final RazorpayProperties properties;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(
            RazorpayClient razorpayClient,
            RazorpayProperties properties,
            SubscriptionService subscriptionService,
            SubscriptionRepository subscriptionRepository,
            PaymentEventRepository paymentEventRepository,
            ObjectMapper objectMapper) {
        this.razorpayClient = razorpayClient;
        this.properties = properties;
        this.subscriptionService = subscriptionService;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentLinkResponse initiatePayment(UUID accountId) throws IOException, InterruptedException {
        String planId = properties.planId();
        if (planId == null || planId.isBlank()) {
            throw new IllegalStateException("Razorpay not configured — set BONACA_RAZORPAY_PLAN_ID env var");
        }
        RazorpayClient.CreateSubscriptionResult result = razorpayClient.createSubscription(planId);
        Subscription subscription = subscriptionRepository
                .findByAccountId(accountId)
                .orElseThrow(() -> new SubscriptionNotFoundException("No subscription for account: " + accountId));
        subscription.setRazorpaySubscriptionId(result.subscriptionId());
        subscriptionRepository.save(subscription);
        return new PaymentLinkResponse(properties.keyId(), result.subscriptionId(), result.shortUrl());
    }

    @Transactional
    public void handleWebhook(
            String razorpayEventId,
            String eventType,
            String subscriptionId,
            String paymentId,
            String payload,
            UUID accountId) {
        if (razorpayEventId != null && paymentEventRepository.existsByRazorpayEventId(razorpayEventId)) {
            log.info("Razorpay event {} already processed — skipping", razorpayEventId);
            return;
        }
        paymentEventRepository.save(new PaymentEvent(
                accountId,
                razorpayEventId != null ? razorpayEventId : UUID.randomUUID().toString(),
                eventType,
                subscriptionId,
                paymentId,
                payload));

        switch (eventType) {
            case "subscription.activated", "subscription.charged" ->
                    subscriptionService.activate(accountId, Instant.now());
            case "subscription.renewed" -> {
                Instant nextBilling = extractNextBillingAt(payload);
                subscriptionService.recordRenewal(accountId, nextBilling);
            }
            case "subscription.cancelled" ->
                    subscriptionService.cancel(accountId);
            case "subscription.halted" ->
                    subscriptionService.markExpiring(accountId);
            default ->
                    log.debug("Razorpay event {} for account {} — no subscription state change", eventType, accountId);
        }
    }

    private Instant extractNextBillingAt(String payload) {
        try {
            JsonNode subEntity = objectMapper.readTree(payload)
                    .path("payload").path("subscription").path("entity");
            String currentEnd = subEntity.path("current_end").asText(null);
            if (currentEnd != null && !currentEnd.isEmpty()) {
                return Instant.ofEpochSecond(Long.parseLong(currentEnd));
            }
        } catch (Exception e) {
            log.warn("Could not parse current_end from Razorpay subscription.renewed payload: {}", e.getMessage());
        }
        return null;
    }
}
