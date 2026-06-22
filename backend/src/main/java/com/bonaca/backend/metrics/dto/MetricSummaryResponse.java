package com.bonaca.backend.metrics.dto;

/** One entry per metric type with data in range — see MetricsController's member-metrics-summary endpoint. */
public record MetricSummaryResponse(
        String metricType,
        double value,
        String unit,
        double rangeMin,
        double rangeMax,
        String trendLabel,
        double deviationScore) {
}
