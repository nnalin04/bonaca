package com.bonaca.backend.metrics.dto;

import java.util.List;

/**
 * hasData=false (average/rangeMin/rangeMax/trendLabel all null, chartValues empty) is the
 * explicit "no recent data" shape PRD §12 requires — never a fabricated value.
 */
public record MetricDetailResponse(
        String metricType,
        boolean hasData,
        Double average,
        String unit,
        Double rangeMin,
        Double rangeMax,
        List<Double> chartValues,
        String trendLabel,
        String insightText) {
}
