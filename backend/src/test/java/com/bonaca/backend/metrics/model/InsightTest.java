package com.bonaca.backend.metrics.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InsightTest {

    @Test
    void accessorsReflectConstructorArguments() {
        UUID memberId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        Insight insight = new Insight(memberId, MetricType.HEART_RATE, "text", InsightKind.TREND, today);

        assertThat(insight.getMemberId()).isEqualTo(memberId);
        assertThat(insight.getMetricType()).isEqualTo(MetricType.HEART_RATE);
        assertThat(insight.getGeneratedText()).isEqualTo("text");
        assertThat(insight.getKind()).isEqualTo(InsightKind.TREND);
        assertThat(insight.getInsightDate()).isEqualTo(today);
        assertThat(insight.getCreatedAt()).isNotNull();
        assertThat(insight.getId()).isNull();
    }

    @Test
    void updateTextOverwritesTheGeneratedTextInPlace() {
        Insight insight = new Insight(UUID.randomUUID(), MetricType.HEART_RATE, "old", InsightKind.TREND, LocalDate.now());

        insight.updateText("new");

        assertThat(insight.getGeneratedText()).isEqualTo("new");
    }

    @Test
    void updateKindOverwritesTheKindInPlace() {
        Insight insight = new Insight(UUID.randomUUID(), MetricType.HEART_RATE, "text", InsightKind.TREND, LocalDate.now());

        insight.updateKind(InsightKind.ANOMALY);

        assertThat(insight.getKind()).isEqualTo(InsightKind.ANOMALY);
    }
}
