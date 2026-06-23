import type { MetricDetailSummary } from '@/features/metrics/model/metricDetailSummary';
import type { MetricReading } from '@/types';
import type {
  MetricDetailResponse,
  MetricSummaryResponse,
} from '@/types/metrics';

const now = () => new Date().toISOString();

export function metricSummaryToReading(
  memberId: string,
  response: MetricSummaryResponse,
): MetricReading {
  return {
    id: `summary-${response.metricType}`,
    memberId,
    metricType: response.metricType,
    value: response.value,
    unit: response.unit,
    recordedAt: now(),
    sourceDeviceId: 'backend',
    trendLabel: response.trendLabel ?? undefined,
    rangeMin: response.rangeMin,
    rangeMax: response.rangeMax,
  };
}

export function metricDetailToSummary(
  memberId: string,
  response: MetricDetailResponse,
): MetricDetailSummary | null {
  if (!response.hasData || response.average === null) return null;

  return {
    average: {
      id: `detail-${response.metricType}`,
      memberId,
      metricType: response.metricType,
      value: response.average,
      unit: response.unit ?? '',
      recordedAt: now(),
      sourceDeviceId: 'backend',
      trendLabel: response.trendLabel ?? undefined,
      rangeMin: response.rangeMin ?? undefined,
      rangeMax: response.rangeMax ?? undefined,
    },
    chartValues: response.chartValues,
    chartAxisMin: response.rangeMin ?? undefined,
    chartAxisMax: response.rangeMax ?? undefined,
  };
}
