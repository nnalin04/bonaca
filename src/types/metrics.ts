import type { MetricTrendLabel, MetricType } from '@/types';

export type MetricRangeQuery = '24h' | '7d' | '30d';

export interface MetricSummaryResponse {
  metricType: MetricType;
  value: number;
  unit: string;
  rangeMin: number;
  rangeMax: number;
  trendLabel: MetricTrendLabel | null;
  deviationScore: number;
}

export interface MetricDetailResponse {
  metricType: MetricType;
  hasData: boolean;
  average: number | null;
  unit: string | null;
  rangeMin: number | null;
  rangeMax: number | null;
  chartValues: number[];
  trendLabel: MetricTrendLabel | null;
  insightText: string | null;
}

export interface InsightResponse {
  id: string;
  metricType: MetricType | null;
  generatedText: string;
  kind: 'trend' | 'anomaly';
  insightDate: string;
}
