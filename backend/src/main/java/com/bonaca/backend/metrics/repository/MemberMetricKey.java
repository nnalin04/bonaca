package com.bonaca.backend.metrics.repository;

import com.bonaca.backend.metrics.model.MetricType;
import java.util.UUID;

/** Projection for distinct (member, metricType) pairs with recent data — see MetricReadingRepository. */
public record MemberMetricKey(UUID memberId, MetricType metricType) {
}
