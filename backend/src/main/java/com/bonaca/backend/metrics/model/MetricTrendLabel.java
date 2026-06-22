package com.bonaca.backend.metrics.model;

/** Matches src/types/index.ts's MetricTrendLabel union — relative-only comparison language per PRD §5. */
public enum MetricTrendLabel {
    HIGHER_THAN_USUAL,
    LOWER_THAN_USUAL,
    SAME_AS_USUAL
}
