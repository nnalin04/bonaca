package com.bonaca.backend.metrics.dto;

import com.bonaca.backend.metrics.model.Insight;
import java.time.LocalDate;
import java.util.UUID;

public record InsightResponse(UUID id, String metricType, String generatedText, String kind, LocalDate insightDate) {

    public static InsightResponse from(Insight insight) {
        return new InsightResponse(
                insight.getId(),
                insight.getMetricType() == null ? null : insight.getMetricType().name().toLowerCase(),
                insight.getGeneratedText(),
                insight.getKind().name().toLowerCase(),
                insight.getInsightDate());
    }
}
