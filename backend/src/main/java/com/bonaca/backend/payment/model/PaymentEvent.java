package com.bonaca.backend.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "payment_events")
public class PaymentEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "razorpay_event_id", nullable = false, unique = true, length = 60)
    private String razorpayEventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "razorpay_subscription_id", length = 60)
    private String razorpaySubscriptionId;

    @Column(name = "razorpay_payment_id", length = 60)
    private String razorpayPaymentId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt = Instant.now();

    protected PaymentEvent() {}

    public PaymentEvent(
            UUID accountId,
            String razorpayEventId,
            String eventType,
            String razorpaySubscriptionId,
            String razorpayPaymentId,
            String payload) {
        this.accountId = accountId;
        this.razorpayEventId = razorpayEventId;
        this.eventType = eventType;
        this.razorpaySubscriptionId = razorpaySubscriptionId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.payload = payload;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getRazorpayEventId() { return razorpayEventId; }
    public String getEventType() { return eventType; }
    public String getRazorpaySubscriptionId() { return razorpaySubscriptionId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public String getPayload() { return payload; }
    public Instant getProcessedAt() { return processedAt; }
}
