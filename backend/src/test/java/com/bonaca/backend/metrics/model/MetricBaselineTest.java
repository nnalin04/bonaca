package com.bonaca.backend.metrics.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MetricBaselineTest {

    @Test
    void accessorsReflectConstructorArguments() {
        UUID memberId = UUID.randomUUID();
        Instant computedAt = Instant.now();
        MetricBaseline baseline = new MetricBaseline(memberId, MetricType.HEART_RATE, 70.0, 5.0, 14, computedAt);

        assertThat(baseline.getMemberId()).isEqualTo(memberId);
        assertThat(baseline.getMetricType()).isEqualTo(MetricType.HEART_RATE);
        assertThat(baseline.getBaselineMean()).isEqualTo(70.0);
        assertThat(baseline.getBaselineStddev()).isEqualTo(5.0);
        assertThat(baseline.getValidDayCount()).isEqualTo(14);
        assertThat(baseline.getComputedAt()).isEqualTo(computedAt);
        assertThat(baseline.getId()).isNull();
    }

    @Test
    void updateOverwritesTheComputedFieldsInPlace() {
        MetricBaseline baseline = new MetricBaseline(UUID.randomUUID(), MetricType.HEART_RATE, 70.0, 5.0, 14, Instant.now());
        Instant newComputedAt = Instant.now().plusSeconds(60);

        baseline.update(72.0, 6.0, 15, newComputedAt);

        assertThat(baseline.getBaselineMean()).isEqualTo(72.0);
        assertThat(baseline.getBaselineStddev()).isEqualTo(6.0);
        assertThat(baseline.getValidDayCount()).isEqualTo(15);
        assertThat(baseline.getComputedAt()).isEqualTo(newComputedAt);
    }
}
