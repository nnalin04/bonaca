package com.bonaca.backend.payment.service;

import com.bonaca.backend.payment.config.RazorpayProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(RazorpayProperties.class)
public class RazorpayClient {

    private static final Logger log = LoggerFactory.getLogger(RazorpayClient.class);
    private static final String RAZORPAY_BASE = "https://api.razorpay.com/v1";

    public record CreateSubscriptionResult(String subscriptionId, String shortUrl) {}

    private final RazorpayProperties properties;
    private final HttpClient httpClient;

    public RazorpayClient(RazorpayProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CreateSubscriptionResult createSubscription(String planId) throws IOException, InterruptedException {
        requireConfigured();
        String body = "{\"plan_id\":\"" + planId + "\",\"total_count\":120,\"quantity\":1,\"customer_notify\":0}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RAZORPAY_BASE + "/subscriptions"))
                .header("Authorization", "Basic " + basicAuth())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            log.error("Razorpay createSubscription failed HTTP {}: {}", response.statusCode(), response.body());
            throw new IOException("Razorpay API returned HTTP " + response.statusCode());
        }
        return parseCreateSubscriptionResult(response.body());
    }

    private void requireConfigured() {
        if (properties.keyId() == null || properties.keyId().isBlank()) {
            throw new IllegalStateException("Razorpay not configured — set BONACA_RAZORPAY_KEY_ID env var");
        }
    }

    private String basicAuth() {
        String credentials = properties.keyId() + ":" + properties.keySecret();
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static CreateSubscriptionResult parseCreateSubscriptionResult(String json) {
        String id = extractJsonString(json, "id");
        String shortUrl = extractJsonString(json, "short_url");
        if (id == null) {
            throw new IllegalStateException("Razorpay response missing id: " + json);
        }
        return new CreateSubscriptionResult(id, shortUrl != null ? shortUrl : "");
    }

    static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        return end == -1 ? null : json.substring(start, end);
    }
}
