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
@Table(name = "invites")
public class Invite {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "inviter_account_id", nullable = false)
    private UUID inviterAccountId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_offered", nullable = false, length = 20)
    private MemberRole roleOffered;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InviteStatus status = InviteStatus.PENDING;

    @Column(name = "accepted_member_id")
    private UUID acceptedMemberId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Invite() {
    }

    public Invite(UUID inviterAccountId, String phoneNumber, MemberRole roleOffered) {
        this.inviterAccountId = inviterAccountId;
        this.phoneNumber = phoneNumber;
        this.roleOffered = roleOffered;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInviterAccountId() {
        return inviterAccountId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public MemberRole getRoleOffered() {
        return roleOffered;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public UUID getAcceptedMemberId() {
        return acceptedMemberId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markAccepted(UUID memberId) {
        this.status = InviteStatus.ACCEPTED;
        this.acceptedMemberId = memberId;
    }

    public void cancel() {
        this.status = InviteStatus.CANCELLED;
    }
}
