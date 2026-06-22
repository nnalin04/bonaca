package com.bonaca.backend.metrics.repository;

import com.bonaca.backend.metrics.model.MetricReading;
import com.bonaca.backend.metrics.model.MetricType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetricReadingRepository extends JpaRepository<MetricReading, UUID> {

    List<MetricReading> findByMemberIdAndRecordedAtAfterOrderByRecordedAtAsc(UUID memberId, Instant since);

    List<MetricReading> findByMemberIdAndMetricTypeAndRecordedAtAfterOrderByRecordedAtAsc(
            UUID memberId, MetricType metricType, Instant since);

    Optional<MetricReading> findTopByMemberIdAndMetricTypeOrderByRecordedAtDesc(UUID memberId, MetricType metricType);

    @Query(
            "SELECT DISTINCT new com.bonaca.backend.metrics.repository.MemberMetricKey(r.memberId, r.metricType) "
                    + "FROM MetricReading r WHERE r.recordedAt > :since")
    List<MemberMetricKey> findDistinctMemberMetricPairsSince(@Param("since") Instant since);
}
