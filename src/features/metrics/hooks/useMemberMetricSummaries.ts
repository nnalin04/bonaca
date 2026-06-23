import { useEffect, useMemo, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import { metricSummaryToReading } from '@/features/metrics/model/metricMappers';
import { ApiError, getMemberMetricSummaries } from '@/lib/api';
import type { MetricReading } from '@/types';
import type { MetricRangeQuery, MetricSummaryResponse } from '@/types/metrics';

interface UseMemberMetricSummariesResult {
  readings: MetricReading[];
  isLoading: boolean;
  errorMessage: string | null;
}

export function useMemberMetricSummaries(
  memberId: string,
  range: MetricRangeQuery = '7d',
): UseMemberMetricSummariesResult {
  const { accessToken } = useAuth();
  const [responses, setResponses] = useState<MetricSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken) {
      Promise.resolve().then(() => {
        if (!cancelled) {
          setResponses([]);
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
    getMemberMetricSummaries(accessToken, memberId, range)
      .then((result) => {
        if (!cancelled) setResponses(result);
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setErrorMessage(
            error instanceof ApiError
              ? error.message
              : 'Could not load metrics.',
          );
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, memberId, range]);

  const readings = useMemo(
    () =>
      responses.map((response) => metricSummaryToReading(memberId, response)),
    [memberId, responses],
  );

  return { readings, isLoading, errorMessage };
}
