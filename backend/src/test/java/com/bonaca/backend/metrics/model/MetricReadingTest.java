package com.bonaca.backend.metrics.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MetricReadingTest {

    @Test
    void accessorsReflectConstructorArguments() {
        UUID memberId = UUID.randomUUID();
        Instant recordedAt = Instant.now();
        MetricReading reading = new MetricReading(memberId, MetricType.HEART_RATE, 72.0, "bpm", recordedAt, "device-1");

        assertThat(reading.getMemberId()).isEqualTo(memberId);
        assertThat(reading.getMetricType()).isEqualTo(MetricType.HEART_RATE);
        assertThat(reading.getValue()).isEqualTo(72.0);
        assertThat(reading.getUnit()).isEqualTo("bpm");
        assertThat(reading.getRecordedAt()).isEqualTo(recordedAt);
        assertThat(reading.getSourceDeviceId()).isEqualTo("device-1");
        assertThat(reading.getCreatedAt()).isNotNull();
        assertThat(reading.getId()).isNull();
    }
}
