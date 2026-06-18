import type { Insight, MetricReading, MetricType } from '@/types';

/**
 * Mock data for Metric Details (Figma node 197:1137). Standing in for the future
 * Supabase-backed metric-history fetch — shape mirrors the real domain types so
 * swapping in real data later is a drop-in.
 */
export interface MetricDetailSummary {
  average: MetricReading;
  chartValues: number[];
}

const now = new Date().toISOString();

function reading(
  metricType: MetricType,
  value: number,
  unit: string,
  rangeMin: number,
  rangeMax: number,
): MetricReading {
  return {
    id: `summary-${metricType}`,
    memberId: 'member-self',
    metricType,
    value,
    unit,
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    rangeMin,
    rangeMax,
  };
}

const heartRateChart = [
  0.25, 0.3, 0.22, 0.28, 0.2, 0.32, 0.35, 0.4, 0.5, 0.45, 0.42, 0.4, 0.38, 0.55,
  0.5, 0.6, 0.65, 0.58, 0.7, 0.75, 0.68, 0.8, 1, 0.9, 0.78, 1, 0.6, 0.55, 0.5,
  0.42,
];

const defaultChart = [
  0.3, 0.5, 0.4, 0.6, 0.45, 0.7, 0.55, 0.8, 0.6, 0.9, 0.65, 0.75, 0.5, 0.4, 0.6,
  0.3, 0.5, 0.7, 0.55, 0.8,
];

export const metricDetailSummaries: Record<MetricType, MetricDetailSummary> = {
  heart_rate: {
    average: reading('heart_rate', 120, 'bpm', 78, 148),
    chartValues: heartRateChart,
  },
  heart_rate_variability: {
    average: reading('heart_rate_variability', 52, 'ms', 38, 66),
    chartValues: defaultChart,
  },
  blood_oxygen: {
    average: reading('blood_oxygen', 96, '%', 94, 99),
    chartValues: defaultChart,
  },
  respiration_rate: {
    average: reading('respiration_rate', 16, 'breaths/min', 12, 20),
    chartValues: defaultChart,
  },
  sleep: {
    average: reading('sleep', 7.17, 'h', 5.5, 8.5),
    chartValues: defaultChart,
  },
  stress_level: {
    average: reading('stress_level', 0, '', 0, 0),
    chartValues: defaultChart,
  },
  body_temperature: {
    average: reading('body_temperature', 36.7, '°C', 36.1, 37.2),
    chartValues: defaultChart,
  },
  ecg: { average: reading('ecg', 52, 'ms', 38, 66), chartValues: defaultChart },
  blood_glucose: {
    average: reading('blood_glucose', 108, 'mg/dL', 82, 134),
    chartValues: defaultChart,
  },
  vo2_max: {
    average: reading('vo2_max', 42.3, '', 35, 48),
    chartValues: defaultChart,
  },
  steps: {
    average: reading('steps', 8245, 'steps', 2100, 9800),
    chartValues: defaultChart,
  },
  calories: {
    average: reading('calories', 1820, 'calories', 900, 2400),
    chartValues: defaultChart,
  },
  workouts: {
    average: reading('workouts', 4, 'sessions', 0, 1),
    chartValues: defaultChart,
  },
  training_load: {
    average: reading('training_load', 0, '', 0, 0),
    chartValues: defaultChart,
  },
  routine_adherence: {
    average: reading('routine_adherence', 80, '%', 40, 100),
    chartValues: defaultChart,
  },
  screen_time: {
    average: reading('screen_time', 6.5, 'h', 1, 8),
    chartValues: defaultChart,
  },
  outdoor_time: {
    average: reading('outdoor_time', 2.33, 'h', 0, 4),
    chartValues: defaultChart,
  },
  last_active_location: {
    average: reading('last_active_location', 0, '', 0, 0),
    chartValues: defaultChart,
  },
};

export const metricInsights: Partial<Record<MetricType, Insight>> = {
  heart_rate: {
    id: 'insight-heart-rate',
    memberId: 'member-self',
    metricType: 'heart_rate',
    generatedText:
      'Heart rate increased during the morning hours, likely during active time.',
    date: now,
    kind: 'trend',
  },
  steps: {
    id: 'insight-steps',
    memberId: 'member-self',
    metricType: 'steps',
    generatedText: 'Step count has been trending lower than usual this week.',
    date: now,
    kind: 'trend',
  },
  screen_time: {
    id: 'insight-screen-time',
    memberId: 'member-self',
    metricType: 'screen_time',
    generatedText:
      'Screen time has been higher than usual in the evenings this week.',
    date: now,
    kind: 'trend',
  },
};
