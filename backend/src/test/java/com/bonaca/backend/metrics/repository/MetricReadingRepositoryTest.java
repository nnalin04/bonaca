package com.bonaca.backend.metrics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MetricReadingRepositoryTest {

    @Autowired
    private MetricReadingRepository metricReadingRepository;

    @Test
    void findByMemberIdAndRecordedAtAfterReturnsOnlyReadingsInsideTheWindowOrderedAscending() {
        UUID memberId = UUID.randomUUID();
        Instant now = Instant.now();
        metricReadingRepository.saveAndFlush(
                new MetricReading(memberId, MetricType.HEART_RATE, 60.0, "bpm", now.minus(10, ChronoUnit.DAYS), "d1"));
        metricReadingRepository.saveAndFlush(
                new MetricReading(memberId, MetricType.HEART_RATE, 70.0, "bpm", now.minus(2, ChronoUnit.DAYS), "d1"));
        metricReadingRepository.saveAndFlush(
                new MetricReading(memberId, MetricType.HEART_RATE, 80.0, "bpm", now.minus(1, ChronoUnit.DAYS), "d1"));

        List<MetricReading> result = metricReadingRepository.findByMemberIdAndRecordedAtAfterOrderByRecordedAtAsc(
                memberId, now.minus(7, ChronoUnit.DAYS));

        assertThat(result).extracting(MetricReading::getValue).containsExactly(70.0, 80.0);
    }

    @Test
    void findByMemberIdAndMetricTypeAndRecordedAtAfterFiltersByType() {
        UUID memberId = UUID.randomUUID();
        Instant now = Instant.now();
        metricReadingRepository.saveAndFlush(new MetricReading(memberId, MetricType.HEART_RATE, 70.0, "bpm", now, "d1"));
        metricReadingRepository.saveAndFlush(new MetricReading(memberId, MetricType.STEPS, 5000.0, "steps", now, "d1"));

        List<MetricReading> result = metricReadingRepository.findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(
                memberId, MetricType.HEART_RATE, now.minus(1, ChronoUnit.DAYS));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMetricType()).isEqualTo(MetricType.HEART_RATE);
    }

    @Test
    void findTopByMemberIdAndMetricTypeReturnsTheMostRecentReading() {
        UUID memberId = UUID.randomUUID();
        Instant now = Instant.now();
        metricReadingRepository.saveAndFlush(
                new MetricReading(memberId, MetricType.HEART_RATE, 60.0, "bpm", now.minus(2, ChronoUnit.DAYS), "d1"));
        metricReadingRepository.saveAndFlush(
                new MetricReading(memberId, MetricType.HEART_RATE, 90.0, "bpm", now, "d1"));

        var result = metricReadingRepository.findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(memberId, MetricType.HEART_RATE);

        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo(90.0);
    }

    @Test
    void findDistinctMemberMetricPairsSinceReturnsEachPairOnceEvenWithMultipleReadings() {
        UUID memberId = UUID.randomUUID();
        Instant now = Instant.now();
        metricReadingRepository.saveAndFlush(new MetricReading(memberId, MetricType.HEART_RATE, 60.0, "bpm", now, "d1"));
        metricReadingRepository.saveAndFlush(new MetricReading(memberId, MetricType.HEART_RATE, 65.0, "bpm", now, "d1"));
        metricReadingRepository.saveAndFlush(new MetricReading(memberId, MetricType.STEPS, 5000.0, "steps", now, "d1"));

        List<MemberMetricKey> pairs = metricReadingRepository.findDistinctMemberMetricPairsSince(now.minus(1, ChronoUnit.DAYS));

        assertThat(pairs).containsExactlyInAnyOrder(
                new MemberMetricKey(memberId, MetricType.HEART_RATE), new MemberMetricKey(memberId, MetricType.STEPS));
    }
}
