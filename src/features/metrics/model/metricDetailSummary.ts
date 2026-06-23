import type { MetricReading } from '@/types';

export interface MetricDetailSummary {
  average: MetricReading;
  chartValues: number[];
  chartAxisMin?: number;
  chartAxisMax?: number;
}
