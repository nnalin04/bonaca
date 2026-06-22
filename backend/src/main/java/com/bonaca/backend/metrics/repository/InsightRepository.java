package com.bonaca.backend.metrics.repository;

import com.bonaca.backend.metrics.model.Insight;
import com.bonaca.backend.metrics.model.InsightKind;
import com.bonaca.backend.metrics.model.MetricType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightRepository extends JpaRepository<Insight, UUID> {

    List<Insight> findByMemberIdOrderByInsightDateDesc(UUID memberId);

    Optional<Insight> findByMemberIdAndMetricTypeAndInsightDate(UUID memberId, MetricType metricType, LocalDate insightDate);

    Optional<Insight> findByMemberIdAndMetricTypeIsNullAndInsightDate(UUID memberId, LocalDate insightDate);

    /** Used by notifications.service.NotificationGenerationService's nightly anomaly scan. */
    List<Insight> findByInsightDateAndKind(LocalDate insightDate, InsightKind kind);
}
