package com.bonaca.backend.subscriptions.model;

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
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "renewed_at")
    private Instant renewedAt;

    @Column(name = "razorpay_subscription_id", length = 60)
    private String razorpaySubscriptionId;

    @Column(name = "next_billing_at")
    private Instant nextBillingAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Subscription() {
    }

    public Subscription(UUID accountId, SubscriptionStatus status, Instant trialEndsAt) {
        this.accountId = accountId;
        this.status = status;
        this.trialEndsAt = trialEndsAt;
    }

    public void markActive(Instant renewedAt) {
        this.status = SubscriptionStatus.ACTIVE;
        this.renewedAt = renewedAt;
    }

    public void markExpiring() {
        this.status = SubscriptionStatus.EXPIRING;
    }

    public void markExpired() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void markCancelled() {
        this.status = SubscriptionStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public Instant getRenewedAt() {
        return renewedAt;
    }

    public String getRazorpaySubscriptionId() {
        return razorpaySubscriptionId;
    }

    public void setRazorpaySubscriptionId(String razorpaySubscriptionId) {
        this.razorpaySubscriptionId = razorpaySubscriptionId;
    }

    public Instant getNextBillingAt() {
        return nextBillingAt;
    }

    public void setNextBillingAt(Instant nextBillingAt) {
        this.nextBillingAt = nextBillingAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
