package com.bonaca.backend.wearable.service;

import com.bonaca.backend.members.exception.MemberNotFoundException;
import com.bonaca.backend.members.repository.MemberRepository;
import com.bonaca.backend.metrics.model.MetricType;
import com.bonaca.backend.metrics.service.MetricIngestionService;
import com.bonaca.backend.wearable.model.WearableConnection;
import com.bonaca.backend.wearable.repository.WearableConnectionRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WearableService {

    private static final Logger log = LoggerFactory.getLogger(WearableService.class);

    private final SpikeApiClient spikeApiClient;
    private final WearableConnectionRepository connectionRepository;
    private final MemberRepository memberRepository;
    private final MetricIngestionService metricIngestionService;

    public WearableService(
            SpikeApiClient spikeApiClient,
            WearableConnectionRepository connectionRepository,
            MemberRepository memberRepository,
            MetricIngestionService metricIngestionService) {
        this.spikeApiClient = spikeApiClient;
        this.connectionRepository = connectionRepository;
        this.memberRepository = memberRepository;
        this.metricIngestionService = metricIngestionService;
    }

    @Transactional
    public WearableConnection getOrCreateConnection(UUID memberId) throws IOException, InterruptedException {
        Optional<WearableConnection> existing = connectionRepository.findByMemberId(memberId);
        if (existing.isPresent()) {
            return existing.get();
        }
        memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found: " + memberId));
        SpikeApiClient.CreateUserResult result = spikeApiClient.createUser(memberId);
        WearableConnection connection = new WearableConnection(memberId, result.spikeUserId(), result.connectUrl());
        return connectionRepository.save(connection);
    }

    public Optional<WearableConnection> getConnection(UUID memberId) {
        return connectionRepository.findByMemberId(memberId);
    }

    // WEARABLE-3: Delete DB row first, then attempt Spike API — user can't be stuck with
    // an undeleteable connection if the Spike call fails.
    @Transactional
    public void disconnect(UUID memberId) {
        WearableConnection connection = connectionRepository
                .findByMemberId(memberId)
                .orElseThrow(() -> new MemberNotFoundException("No wearable connection for member: " + memberId));
        connectionRepository.delete(connection);
        connectionRepository.flush();
        try {
            spikeApiClient.deleteUser(connection.getSpikeUserId());
        } catch (Exception e) {
            log.warn("Spike deleteUser failed for spikeUserId {} (DB row already deleted): {}",
                    connection.getSpikeUserId(), e.getMessage());
        }
    }

    @Transactional
    public void processWebhookEvent(String spikeUserId, String eventType, String payload) {
        Optional<WearableConnection> connectionOpt = connectionRepository.findBySpikeUserId(spikeUserId);
        if (connectionOpt.isEmpty()) {
            log.warn("Received Spike webhook for unknown spikeUserId: {}", spikeUserId);
            return;
        }
        WearableConnection connection = connectionOpt.get();
        connection.setLastSyncedAt(Instant.now());

        // WEARABLE-1: Handle both connect and disconnect events
        if ("CONNECTED".equals(eventType) || "connected".equals(eventType)) {
            connection.setStatus("CONNECTED");
            connection.setConnectedAt(Instant.now());
        } else if ("DISCONNECTED".equals(eventType) || "disconnected".equals(eventType)
                || "revoked".equals(eventType) || "expired".equals(eventType)) {
            connection.setStatus("DISCONNECTED");
            connection.setConnectedAt(null);
        }
        connectionRepository.save(connection);

        if (eventType != null && (eventType.startsWith("daily")
                || eventType.startsWith("workout")
                || eventType.startsWith("sleep")
                || eventType.startsWith("heart_rate"))) {
            ingestSpikeData(connection.getMemberId(), spikeUserId, eventType, payload);
        }
    }

    private void ingestSpikeData(UUID memberId, String spikeUserId, String eventType, String payload) {
        try {
            MetricType metricType = mapEventTypeToMetric(eventType, payload);
            if (metricType == null) {
                log.debug("No metric mapping for Spike event type: {}", eventType);
                return;
            }
            // WEARABLE-2: Null extractValue means missing/unparseable field — skip rather than store 0.0
            Double value = extractValue(payload);
            if (value == null) {
                log.warn("Spike event {} for member {} has missing or unparseable value — skipping ingestion", eventType, memberId);
                return;
            }
            String unit = unitForMetric(metricType);
            metricIngestionService.recordReading(memberId, metricType, value, unit, Instant.now(), spikeUserId);
            log.debug("Ingested {} = {} {} for member {} from Spike", metricType, value, unit, memberId);
        } catch (Exception e) {
            log.error("Failed to ingest Spike data for member {} event {}: {}", memberId, eventType, e.getMessage());
        }
    }

    private static MetricType mapEventTypeToMetric(String eventType, String payload) {
        if (eventType.startsWith("heart_rate")) return MetricType.HEART_RATE;
        if (eventType.startsWith("sleep")) return MetricType.SLEEP;
        if (eventType.startsWith("workout")) return MetricType.WORKOUTS;
        if (eventType.startsWith("daily")) {
            if (payload.contains("\"steps\"")) return MetricType.STEPS;
            if (payload.contains("\"calories\"")) return MetricType.CALORIES;
        }
        return null;
    }

    private static String unitForMetric(MetricType type) {
        return switch (type) {
            case HEART_RATE -> "bpm";
            case SLEEP -> "hours";
            case WORKOUTS -> "count";
            case STEPS -> "steps";
            case CALORIES -> "kcal";
            default -> "units";
        };
    }

    // WEARABLE-2: Returns null when value is missing or cannot be parsed, instead of 0.0.
    private static Double extractValue(String payload) {
        String search = "\"value\":";
        int idx = payload.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        int end = start;
        while (end < payload.length() && (Character.isDigit(payload.charAt(end))
                || payload.charAt(end) == '.' || payload.charAt(end) == '-')) {
            end++;
        }
        if (start == end) return null;
        try {
            return Double.parseDouble(payload.substring(start, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
