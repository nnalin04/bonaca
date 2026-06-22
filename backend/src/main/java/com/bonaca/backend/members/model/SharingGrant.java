package com.bonaca.backend.members.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "sharing_grants")
public class SharingGrant {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "granter_member_id", nullable = false)
    private UUID granterMemberId;

    @Column(name = "grantee_member_id", nullable = false)
    private UUID granteeMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private SharingScope scope;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SharingGrant() {
    }

    public SharingGrant(UUID granterMemberId, UUID granteeMemberId, SharingScope scope, boolean visible) {
        this.granterMemberId = granterMemberId;
        this.granteeMemberId = granteeMemberId;
        this.scope = scope;
        this.visible = visible;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGranterMemberId() {
        return granterMemberId;
    }

    public UUID getGranteeMemberId() {
        return granteeMemberId;
    }

    public SharingScope getScope() {
        return scope;
    }

    public boolean isVisible() {
        return visible;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
