package com.bonaca.backend.payment.controller;

import com.bonaca.backend.payment.config.RazorpayProperties;
import com.bonaca.backend.payment.service.PaymentService;
import com.bonaca.backend.subscriptions.repository.SubscriptionRepository;
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

    public RazorpayWebhookController(
            PaymentService paymentService,
            RazorpayProperties properties,
            SubscriptionRepository subscriptionRepository) {
        this.paymentService = paymentService;
        this.properties = properties;
        this.subscriptionRepository = subscriptionRepository;
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
            String eventType = extractJsonString(rawBody, "event");
            String subscriptionId = extractSubscriptionId(rawBody);
            String paymentId = extractPaymentId(rawBody);

            if (subscriptionId == null) {
                log.warn("Razorpay webhook missing subscription id for event {}", eventType);
                return ResponseEntity.ok().build();
            }

            UUID accountId = subscriptionRepository.findByRazorpaySubscriptionId(subscriptionId)
                    .map(s -> s.getAccountId())
                    .orElse(null);
            if (accountId == null) {
                log.warn("Razorpay webhook for unknown subscription {}", subscriptionId);
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
        if (secret == null || secret.isBlank()) return true;
        if (signature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = hexToBytes(signature);
            return MessageDigest.isEqual(computed, supplied);
        } catch (Exception e) {
            log.error("Razorpay signature validation error: {}", e.getMessage());
            return false;
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // Razorpay nests subscription id under payload.subscription.entity.id
    private static String extractSubscriptionId(String json) {
        // Try nested path first, then flat
        int subIdx = json.indexOf("\"subscription\"");
        if (subIdx != -1) {
            String sub = json.substring(subIdx);
            String id = extractJsonString(sub, "id");
            if (id != null && id.startsWith("sub_")) return id;
        }
        return null;
    }

    private static String extractPaymentId(String json) {
        int payIdx = json.indexOf("\"payment\"");
        if (payIdx != -1) {
            String pay = json.substring(payIdx);
            String id = extractJsonString(pay, "id");
            if (id != null && id.startsWith("pay_")) return id;
        }
        return null;
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        return end == -1 ? null : json.substring(start, end);
    }
}
