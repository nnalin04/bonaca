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
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * metricType is nullable: a composite, account-wide insight like the Routine Consistency Score
 * (PRD §5) isn't tied to a single metric. Note the uniqueConstraint below only enforces
 * uniqueness when metricType is non-null — SQL NULLs are never equal to each other under a
 * standard UNIQUE constraint, on both H2 and Postgres, so InsightGenerationService's upsert for
 * the null-metricType case must look up the existing row explicitly rather than relying on a
 * constraint-violation catch.
 */
@Entity
@Table(
        name = "insights",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "metric_type", "insight_date"}))
public class Insight {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", length = 40)
    private MetricType metricType;

    @Column(name = "generated_text", nullable = false, length = 280)
    private String generatedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private InsightKind kind;

    @Column(name = "insight_date", nullable = false)
    private LocalDate insightDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Insight() {
    }

    public Insight(UUID memberId, MetricType metricType, String generatedText, InsightKind kind, LocalDate insightDate) {
        this.memberId = memberId;
        this.metricType = metricType;
        this.generatedText = generatedText;
        this.kind = kind;
        this.insightDate = insightDate;
    }

    /** Re-running the nightly job on the same day updates the existing row rather than duplicating it. */
    public void updateText(String generatedText) {
        this.generatedText = generatedText;
    }

    /** A re-run can reclassify TREND -> ANOMALY (or back) if the day's z-score crossed the threshold differently. */
    public void updateKind(InsightKind kind) {
        this.kind = kind;
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

    public String getGeneratedText() {
        return generatedText;
    }

    public InsightKind getKind() {
        return kind;
    }

    public LocalDate getInsightDate() {
        return insightDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
