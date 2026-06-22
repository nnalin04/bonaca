package com.bonaca.backend.metrics.repository;

import com.bonaca.backend.metrics.model.MetricBaseline;
import com.bonaca.backend.metrics.model.MetricType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricBaselineRepository extends JpaRepository<MetricBaseline, UUID> {

    Optional<MetricBaseline> findByMemberIdAndMetricType(UUID memberId, MetricType metricType);

    List<MetricBaseline> findByMemberId(UUID memberId);
}
