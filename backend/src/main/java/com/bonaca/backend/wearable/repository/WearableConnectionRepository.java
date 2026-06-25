package com.bonaca.backend.wearable.repository;

import com.bonaca.backend.wearable.model.WearableConnection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WearableConnectionRepository extends JpaRepository<WearableConnection, UUID> {

    Optional<WearableConnection> findByMemberId(UUID memberId);

    Optional<WearableConnection> findBySpikeUserId(String spikeUserId);
}
