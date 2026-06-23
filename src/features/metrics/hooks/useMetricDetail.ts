import { useEffect, useMemo, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import type { MetricDetailSummary } from '@/features/metrics/mockData';
import { metricDetailToSummary } from '@/features/metrics/model/metricMappers';
import { ApiError, getMetricDetail } from '@/lib/api';
import type { MetricType } from '@/types';
import type { MetricDetailResponse, MetricRangeQuery } from '@/types/metrics';

interface UseMetricDetailResult {
  summary: MetricDetailSummary | null;
  insightText: string | null;
  isLoading: boolean;
  errorMessage: string | null;
}

export function useMetricDetail(
  memberId: string,
  metricType: MetricType,
  range: MetricRangeQuery,
): UseMetricDetailResult {
  const { accessToken } = useAuth();
  const [response, setResponse] = useState<MetricDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken) {
      Promise.resolve().then(() => {
        if (!cancelled) {
          setResponse(null);
          setIsLoading(false);
        }
      });
      return () => {
        cancelled = true;
      };
    }

    Promise.resolve().then(() => {
      if (!cancelled) {
        setIsLoading(true);
        setErrorMessage(null);
      }
    });
    getMetricDetail(accessToken, memberId, metricType, range)
      .then((result) => {
        if (!cancelled) setResponse(result);
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setErrorMessage(
            error instanceof ApiError
              ? error.message
              : 'Could not load metric details.',
          );
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, memberId, metricType, range]);

  const summary = useMemo(
    () => (response ? metricDetailToSummary(memberId, response) : null),
    [memberId, response],
  );

  return {
    summary,
    insightText: response?.insightText ?? null,
    isLoading,
    errorMessage,
  };
}
