package com.bonaca.backend.wearable.service;

import com.bonaca.backend.wearable.config.SpikeProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(SpikeProperties.class)
public class SpikeApiClient {

    private static final Logger log = LoggerFactory.getLogger(SpikeApiClient.class);

    public record CreateUserResult(String spikeUserId, String connectUrl) {}

    private final SpikeProperties properties;
    private final HttpClient httpClient;

    public SpikeApiClient(SpikeProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CreateUserResult createUser(UUID memberId) throws IOException, InterruptedException {
        requireConfigured();
        String body = "{\"external_id\":\"" + memberId + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl() + "/team/users"))
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            log.error("Spike createUser failed HTTP {}: {}", response.statusCode(), response.body());
            throw new IOException("Spike API returned HTTP " + response.statusCode());
        }
        return parseCreateUserResult(response.body());
    }

    public boolean deleteUser(String spikeUserId) throws IOException, InterruptedException {
        requireConfigured();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl() + "/team/users/" + spikeUserId))
                .header("Authorization", "Bearer " + properties.apiKey())
                .DELETE()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200 || response.statusCode() == 204;
    }

    private void requireConfigured() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("Spike not configured — set BONACA_SPIKE_API_KEY env var");
        }
    }

    private static CreateUserResult parseCreateUserResult(String json) {
        String userId = extractJsonString(json, "user_id");
        String connectUrl = extractJsonString(json, "connect_url");
        if (userId == null || connectUrl == null) {
            throw new IllegalStateException("Spike response missing user_id or connect_url: " + json);
        }
        return new CreateUserResult(userId, connectUrl);
    }

    static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
