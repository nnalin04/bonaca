package com.bonaca.backend.metrics.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonaca.backend.metrics.model.MetricBaseline;
import com.bonaca.backend.metrics.model.MetricType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Schema contract: UNIQUE (member_id, metric_type) — BaselineService's upsert logic depends on this. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MetricBaselineRepositoryTest {

    @Autowired
    private MetricBaselineRepository metricBaselineRepository;

    @Test
    void findByMemberIdAndMetricTypeReturnsTheMatchingBaseline() {
        UUID memberId = UUID.randomUUID();
        metricBaselineRepository.saveAndFlush(new MetricBaseline(memberId, MetricType.HEART_RATE, 70.0, 5.0, 14, Instant.now()));

        assertThat(metricBaselineRepository.findByMemberIdAndMetricType(memberId, MetricType.HEART_RATE)).isPresent();
    }

    @Test
    void findByMemberIdReturnsEveryBaselineForThatMember() {
        UUID memberId = UUID.randomUUID();
        metricBaselineRepository.saveAndFlush(new MetricBaseline(memberId, MetricType.HEART_RATE, 70.0, 5.0, 14, Instant.now()));
        metricBaselineRepository.saveAndFlush(new MetricBaseline(memberId, MetricType.STEPS, 8000.0, 500.0, 14, Instant.now()));
        metricBaselineRepository.saveAndFlush(
                new MetricBaseline(UUID.randomUUID(), MetricType.HEART_RATE, 65.0, 4.0, 14, Instant.now()));

        assertThat(metricBaselineRepository.findByMemberId(memberId)).hasSize(2);
    }

    @Test
    void memberIdAndMetricTypeMustBeUniqueTogether() {
        UUID memberId = UUID.randomUUID();
        metricBaselineRepository.saveAndFlush(new MetricBaseline(memberId, MetricType.HEART_RATE, 70.0, 5.0, 14, Instant.now()));

        assertThatThrownBy(() -> metricBaselineRepository.saveAndFlush(
                        new MetricBaseline(memberId, MetricType.HEART_RATE, 72.0, 6.0, 14, Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
