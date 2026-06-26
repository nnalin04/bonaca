package com.bonaca.backend.payment.controller;

import com.bonaca.backend.common.HmacUtil;
import com.bonaca.backend.payment.config.RazorpayProperties;
import com.bonaca.backend.payment.service.PaymentService;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@EnableConfigurationProperties(RazorpayProperties.class)
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final PaymentService paymentService;
    private final RazorpayProperties properties;
    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    public RazorpayWebhookController(
            PaymentService paymentService,
            RazorpayProperties properties,
            SubscriptionRepository subscriptionRepository,
            ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.properties = properties;
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<Void> razorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        try {
            if (!validateSignature(rawBody, signature)) {
                log.warn("Razorpay webhook signature mismatch");
                return ResponseEntity.status(401).build();
            }

            JsonNode root = objectMapper.readTree(rawBody);
            String eventType = root.path("event").asText(null);

            // Razorpay nests subscription under payload.subscription.entity
            JsonNode subEntity = root.path("payload").path("subscription").path("entity");
            String subscriptionId = subEntity.path("id").asText(null);
            if (subscriptionId != null && !subscriptionId.startsWith("sub_")) {
                subscriptionId = null;
            }

            // Razorpay nests payment under payload.payment.entity
            JsonNode payEntity = root.path("payload").path("payment").path("entity");
            String paymentId = payEntity.path("id").asText(null);
            if (paymentId != null && !paymentId.startsWith("pay_")) {
                paymentId = null;
            }

            if (subscriptionId == null) {
                log.warn("Razorpay webhook missing subscription id for event {}", eventType);
                return ResponseEntity.ok().build();
            }

            final String finalSubscriptionId = subscriptionId;
            UUID accountId = subscriptionRepository.findByRazorpaySubscriptionId(subscriptionId)
                    .map(s -> s.getAccountId())
                    .orElse(null);
            if (accountId == null) {
                log.warn("Razorpay webhook for unknown subscription {}", finalSubscriptionId);
                return ResponseEntity.ok().build();
            }

            paymentService.handleWebhook(eventId, eventType, subscriptionId, paymentId, rawBody, accountId);
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }

    private boolean validateSignature(String body, String signature) {
        String secret = properties.webhookSecret();
        if (secret == null || secret.isBlank()) return false;
        if (signature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HmacUtil.hexToBytes(signature);
            return MessageDigest.isEqual(computed, supplied);
        } catch (Exception e) {
            log.error("Razorpay signature validation error: {}", e.getMessage());
            return false;
        }
    }
}
