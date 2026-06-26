package com.bonaca.backend.wearable.controller;

import com.bonaca.backend.wearable.config.SpikeProperties;
import com.bonaca.backend.wearable.service.WearableService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@EnableConfigurationProperties(SpikeProperties.class)
public class SpikeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SpikeWebhookController.class);

    private final WearableService wearableService;
    private final SpikeProperties properties;

    public SpikeWebhookController(WearableService wearableService, SpikeProperties properties) {
        this.wearableService = wearableService;
        this.properties = properties;
    }

    @PostMapping("/spike")
    public ResponseEntity<Void> spikeWebhook(
            @RequestBody String rawBody,
            HttpServletRequest request) {
        try {
            String signature = request.getHeader("X-Spike-Signature");
            if (!validateSignature(rawBody, signature)) {
                log.warn("Spike webhook signature mismatch — ignoring");
                return ResponseEntity.status(401).build();
            }
            String spikeUserId = extractField(rawBody, "user_id");
            if (spikeUserId == null) spikeUserId = extractField(rawBody, "spike_user_id");
            String eventType = extractField(rawBody, "type");
            if (eventType == null) eventType = extractField(rawBody, "event_type");

            if (spikeUserId != null && eventType != null) {
                wearableService.processWebhookEvent(spikeUserId, eventType, rawBody);
            } else {
                log.warn("Spike webhook missing user_id or event type in payload");
            }
        } catch (Exception e) {
            log.error("Error processing Spike webhook: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }

    private boolean validateSignature(String body, String signature) {
        String secret = properties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            return false;
        }
        if (signature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = hexToBytes(signature.startsWith("sha256=")
                    ? signature.substring(7) : signature);
            return MessageDigest.isEqual(computed, supplied);
        } catch (Exception e) {
            log.error("Spike signature validation error: {}", e.getMessage());
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

    private static String extractField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        return end == -1 ? null : json.substring(start, end);
    }
}
