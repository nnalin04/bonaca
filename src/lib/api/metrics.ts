import { apiClient } from '@/lib/api/client';
import type {
  InsightResponse,
  MetricDetailResponse,
  MetricRangeQuery,
  MetricSummaryResponse,
} from '@/types/metrics';

export function getMemberMetricSummaries(
  accessToken: string,
  memberId: string,
  range: MetricRangeQuery,
): Promise<MetricSummaryResponse[]> {
  return apiClient.get<MetricSummaryResponse[]>(
    `/api/v1/members/${memberId}/metrics?range=${range}`,
    accessToken,
  ) as Promise<MetricSummaryResponse[]>;
}

export function getMetricDetail(
  accessToken: string,
  memberId: string,
  metricType: string,
  range: MetricRangeQuery,
): Promise<MetricDetailResponse> {
  return apiClient.get<MetricDetailResponse>(
    `/api/v1/members/${memberId}/metrics/${metricType}?range=${range}`,
    accessToken,
  ) as Promise<MetricDetailResponse>;
}

export function listInsights(
  accessToken: string,
  memberId: string,
): Promise<InsightResponse[]> {
  return apiClient.get<InsightResponse[]>(
    `/api/v1/members/${memberId}/insights`,
    accessToken,
  ) as Promise<InsightResponse[]>;
}
