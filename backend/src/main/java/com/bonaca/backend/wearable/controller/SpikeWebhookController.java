package com.bonaca.backend.wearable.controller;

import com.bonaca.backend.common.HmacUtil;
import com.bonaca.backend.wearable.config.SpikeProperties;
import com.bonaca.backend.wearable.service.WearableService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public SpikeWebhookController(
            WearableService wearableService,
            SpikeProperties properties,
            ObjectMapper objectMapper) {
        this.wearableService = wearableService;
        this.properties = properties;
        this.objectMapper = objectMapper;
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

            JsonNode root = objectMapper.readTree(rawBody);
            String spikeUserId = root.path("user_id").asText(null);
            if (spikeUserId == null) spikeUserId = root.path("spike_user_id").asText(null);
            String eventType = root.path("type").asText(null);
            if (eventType == null) eventType = root.path("event_type").asText(null);

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
            byte[] supplied = HmacUtil.hexToBytes(signature.startsWith("sha256=")
                    ? signature.substring(7) : signature);
            return MessageDigest.isEqual(computed, supplied);
        } catch (Exception e) {
            log.error("Spike signature validation error: {}", e.getMessage());
            return false;
        }
    }
}
