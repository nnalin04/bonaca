package com.bonaca.backend.wearable.dto;

import com.bonaca.backend.wearable.model.WearableConnection;
import java.time.Instant;
import java.util.UUID;

public record WearableConnectionResponse(
        UUID memberId,
        String spikeUserId,
        String provider,
        String status,
        String connectUrl,
        Instant connectedAt,
        Instant lastSyncedAt) {

    public static WearableConnectionResponse from(WearableConnection c) {
        return new WearableConnectionResponse(
                c.getMemberId(),
                c.getSpikeUserId(),
                c.getProvider(),
                c.getStatus(),
                c.getConnectUrl(),
                c.getConnectedAt(),
                c.getLastSyncedAt());
    }
}
