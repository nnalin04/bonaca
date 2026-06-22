package com.bonaca.backend.metrics.model;

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
@Table(name = "metric_readings")
public class MetricReading {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 40)
    private MetricType metricType;

    // not "value" — that's a reserved word in H2's SQL grammar. columnDefinition matches the
    // Flyway migration's NUMERIC(10,3) exactly, so Hibernate's ddl-auto=validate (the real
    // Postgres tier) doesn't reject it as a FLOAT/NUMERIC mismatch.
    @Column(name = "metric_value", nullable = false, columnDefinition = "numeric(10,3)")
    private double value;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "source_device_id", length = 120)
    private String sourceDeviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MetricReading() {
    }

    public MetricReading(
            UUID memberId, MetricType metricType, double value, String unit, Instant recordedAt, String sourceDeviceId) {
        this.memberId = memberId;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
        this.sourceDeviceId = sourceDeviceId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getSourceDeviceId() {
        return sourceDeviceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
