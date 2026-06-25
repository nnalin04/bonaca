package com.bonaca.backend.wearable.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "wearable_connections")
public class WearableConnection {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "member_id", nullable = false, unique = true)
    private UUID memberId;

    @Column(name = "spike_user_id", nullable = false, length = 120)
    private String spikeUserId;

    @Column(name = "provider", length = 40)
    private String provider;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "connect_url", columnDefinition = "TEXT")
    private String connectUrl;

    @Column(name = "connected_at")
    private Instant connectedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected WearableConnection() {}

    public WearableConnection(UUID memberId, String spikeUserId, String connectUrl) {
        this.memberId = memberId;
        this.spikeUserId = spikeUserId;
        this.connectUrl = connectUrl;
    }

    public UUID getId() { return id; }
    public UUID getMemberId() { return memberId; }
    public String getSpikeUserId() { return spikeUserId; }
    public String getProvider() { return provider; }
    public String getStatus() { return status; }
    public String getConnectUrl() { return connectUrl; }
    public Instant getConnectedAt() { return connectedAt; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setProvider(String provider) { this.provider = provider; }
    public void setStatus(String status) { this.status = status; }
    public void setConnectUrl(String connectUrl) { this.connectUrl = connectUrl; }
    public void setConnectedAt(Instant connectedAt) { this.connectedAt = connectedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
