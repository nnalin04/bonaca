package com.bonaca.backend.members.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "owner_member_id")
    private UUID ownerMemberId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Account() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerMemberId() {
        return ownerMemberId;
    }

    public void setOwnerMemberId(UUID ownerMemberId) {
        this.ownerMemberId = ownerMemberId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
