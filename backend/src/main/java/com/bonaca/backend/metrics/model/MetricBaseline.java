package com.bonaca.backend.metrics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * One row per (member, metricType) — recomputed nightly by BaselineService, never deleted, only
 * updated in place (see {@link #update}). The unique constraint is declared here (not left to
 * the Flyway migration alone) because BaselineService's upsert logic depends on it being
 * enforced consistently across both the H2 fast tier and the real Postgres tier.
 */
@Entity
@Table(name = "metric_baselines", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "metric_type"}))
public class MetricBaseline {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 40)
    private MetricType metricType;

    @Column(name = "baseline_mean", nullable = false, columnDefinition = "numeric(10,3)")
    private double baselineMean;

    @Column(name = "baseline_stddev", nullable = false, columnDefinition = "numeric(10,3)")
    private double baselineStddev;

    @Column(name = "valid_day_count", nullable = false)
    private int validDayCount;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    protected MetricBaseline() {
    }

    public MetricBaseline(
            UUID memberId, MetricType metricType, double baselineMean, double baselineStddev, int validDayCount,
            Instant computedAt) {
        this.memberId = memberId;
        this.metricType = metricType;
        this.baselineMean = baselineMean;
        this.baselineStddev = baselineStddev;
        this.validDayCount = validDayCount;
        this.computedAt = computedAt;
    }

    public void update(double baselineMean, double baselineStddev, int validDayCount, Instant computedAt) {
        this.baselineMean = baselineMean;
        this.baselineStddev = baselineStddev;
        this.validDayCount = validDayCount;
        this.computedAt = computedAt;
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

    public double getBaselineMean() {
        return baselineMean;
    }

    public double getBaselineStddev() {
        return baselineStddev;
    }

    public int getValidDayCount() {
        return validDayCount;
    }

    public Instant getComputedAt() {
        return computedAt;
    }
}
