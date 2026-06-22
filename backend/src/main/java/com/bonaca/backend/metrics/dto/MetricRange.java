package com.bonaca.backend.metrics.dto;

import com.bonaca.backend.metrics.exception.InvalidMetricRangeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** PRD §10's Metric Detail ranges — 24h/7d/30d, not the 1D/7D/4W/1Y the built frontend currently uses (a known, already-tracked mismatch). */
public enum MetricRange {
    TWENTY_FOUR_HOURS("24h", 1),
    SEVEN_DAYS("7d", 7),
    THIRTY_DAYS("30d", 30);

    private final String queryValue;
    private final int days;

    MetricRange(String queryValue, int days) {
        this.queryValue = queryValue;
        this.days = days;
    }

    public Instant since(Instant now) {
        return now.minus(days, ChronoUnit.DAYS);
    }

    public static MetricRange fromQueryValue(String value) {
        for (MetricRange range : values()) {
            if (range.queryValue.equalsIgnoreCase(value)) {
                return range;
            }
        }
        throw new InvalidMetricRangeException("Unknown range '" + value + "' — expected one of 24h, 7d, 30d");
    }
}
