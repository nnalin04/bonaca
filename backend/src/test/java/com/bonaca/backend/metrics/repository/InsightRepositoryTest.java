package com.bonaca.backend.metrics.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonaca.backend.metrics.model.Insight;
import com.bonaca.backend.metrics.model.InsightKind;
import com.bonaca.backend.metrics.model.MetricType;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class InsightRepositoryTest {

    @Autowired
    private InsightRepository insightRepository;

    @Test
    void findByMemberIdOrdersByInsightDateDescending() {
        UUID memberId = UUID.randomUUID();
        insightRepository.saveAndFlush(
                new Insight(memberId, MetricType.HEART_RATE, "older", InsightKind.TREND, LocalDate.now().minusDays(2)));
        insightRepository.saveAndFlush(
                new Insight(memberId, MetricType.HEART_RATE, "newer", InsightKind.TREND, LocalDate.now()));

        var result = insightRepository.findByMemberIdOrderByInsightDateDesc(memberId);

        assertThat(result).extracting(Insight::getGeneratedText).containsExactly("newer", "older");
    }

    @Test
    void findByMemberIdAndMetricTypeAndInsightDateFindsTheExactRow() {
        UUID memberId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        insightRepository.saveAndFlush(new Insight(memberId, MetricType.STEPS, "text", InsightKind.TREND, today));

        assertThat(insightRepository.findByMemberIdAndMetricTypeAndInsightDate(memberId, MetricType.STEPS, today)).isPresent();
    }

    @Test
    void findByMemberIdAndMetricTypeIsNullAndInsightDateFindsTheCompositeRow() {
        UUID memberId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        insightRepository.saveAndFlush(new Insight(memberId, null, "Routine Consistency: Stable", InsightKind.TREND, today));
        insightRepository.saveAndFlush(new Insight(memberId, MetricType.STEPS, "metric-specific", InsightKind.TREND, today));

        var result = insightRepository.findByMemberIdAndMetricTypeIsNullAndInsightDate(memberId, today);

        assertThat(result).isPresent();
        assertThat(result.get().getGeneratedText()).isEqualTo("Routine Consistency: Stable");
    }

    @Test
    void findByInsightDateAndKindReturnsOnlyThatDatesAnomalies() {
        LocalDate today = LocalDate.now();
        Insight anomaly = insightRepository.saveAndFlush(
                new Insight(UUID.randomUUID(), MetricType.HEART_RATE, "anomaly today", InsightKind.ANOMALY, today));
        insightRepository.saveAndFlush(
                new Insight(UUID.randomUUID(), MetricType.STEPS, "trend today", InsightKind.TREND, today));
        insightRepository.saveAndFlush(new Insight(
                UUID.randomUUID(), MetricType.HEART_RATE, "anomaly yesterday", InsightKind.ANOMALY, today.minusDays(1)));

        var result = insightRepository.findByInsightDateAndKind(today, InsightKind.ANOMALY);

        assertThat(result).extracting(Insight::getId).containsExactly(anomaly.getId());
    }

    @Test
    void memberMetricTypeAndDateMustBeUniqueTogetherWhenMetricTypeIsNotNull() {
        UUID memberId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        insightRepository.saveAndFlush(new Insight(memberId, MetricType.STEPS, "first", InsightKind.TREND, today));

        assertThatThrownBy(() ->
                        insightRepository.saveAndFlush(new Insight(memberId, MetricType.STEPS, "second", InsightKind.TREND, today)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
