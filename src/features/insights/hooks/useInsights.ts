import { useCallback, useEffect, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import { ApiError } from '@/lib/api/client';
import { listInsights } from '@/lib/api/metrics';
import type { InsightResponse } from '@/types/metrics';

interface UseInsightsResult {
  insights: InsightResponse[];
  isLoading: boolean;
  errorMessage: string | null;
  refresh: () => Promise<void>;
}

export function useInsights(memberId: string): UseInsightsResult {
  const { accessToken } = useAuth();
  const [insights, setInsights] = useState<InsightResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!accessToken) {
      setInsights([]);
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const result = await listInsights(accessToken, memberId);
      setInsights(result ?? []);
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : 'Could not load insights.');
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, memberId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return { insights, isLoading, errorMessage, refresh };
}
