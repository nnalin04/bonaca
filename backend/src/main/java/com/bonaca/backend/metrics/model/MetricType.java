package com.bonaca.backend.metrics.model;

import com.bonaca.backend.members.model.SharingScope;

/**
 * Matches src/types/index.ts's MetricType union exactly — see
 * docs/TECHNICAL/METRICS_IMPLEMENTATION_PLAN.md §4 for the category mapping this is built from.
 */
public enum MetricType {
    HEART_RATE(SharingScope.VITALS),
    HEART_RATE_VARIABILITY(SharingScope.VITALS),
    BLOOD_OXYGEN(SharingScope.VITALS),
    RESPIRATION_RATE(SharingScope.VITALS),
    SLEEP(SharingScope.VITALS),
    STRESS_LEVEL(SharingScope.VITALS),
    BODY_TEMPERATURE(SharingScope.VITALS),
    ECG(SharingScope.VITALS),
    BLOOD_GLUCOSE(SharingScope.VITALS),
    VO2_MAX(SharingScope.VITALS),

    STEPS(SharingScope.ACTIVITY),
    CALORIES(SharingScope.ACTIVITY),
    WORKOUTS(SharingScope.ACTIVITY),
    TRAINING_LOAD(SharingScope.ACTIVITY),

    SCREEN_TIME(SharingScope.BEHAVIOUR),
    OUTDOOR_TIME(SharingScope.BEHAVIOUR),
    ROUTINE_ADHERENCE(SharingScope.BEHAVIOUR),
    LAST_ACTIVE_LOCATION(SharingScope.BEHAVIOUR);

    private final SharingScope scope;

    MetricType(SharingScope scope) {
        this.scope = scope;
    }

    public SharingScope scope() {
        return scope;
    }
}
