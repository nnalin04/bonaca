import { useCallback, useEffect, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import { useMembers } from '@/features/members/useMembers';
import { ApiError, getSubscription, mockPay } from '@/lib/api';
import type { SubscriptionResponse } from '@/types/subscriptions';

interface UseCurrentSubscriptionResult {
  accountId: string | null;
  displayName: string;
  subscription: SubscriptionResponse | null;
  isLoading: boolean;
  isPaying: boolean;
  errorMessage: string | null;
  refresh: () => Promise<void>;
  activateMockPayment: () => Promise<boolean>;
}

export function useCurrentSubscription(
  accountIdOverride?: string,
): UseCurrentSubscriptionResult {
  const { accessToken } = useAuth();
  const { self } = useMembers();
  const accountId = accountIdOverride ?? self?.accountId ?? null;
  const displayName = self?.nickname ?? self?.name ?? 'Family plan';
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(
    null,
  );
  const [isLoading, setIsLoading] = useState(true);
  const [isPaying, setIsPaying] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!accessToken || !accountId) {
      setSubscription(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);
    try {
      setSubscription(await getSubscription(accessToken, accountId));
    } catch (error) {
      setErrorMessage(
        getErrorMessage(error, 'Could not load your subscription.'),
      );
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, accountId]);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken || !accountId) {
      Promise.resolve().then(() => {
        if (!cancelled) {
          setSubscription(null);
          setIsLoading(false);
        }
      });
      return () => {
        cancelled = true;
      };
    }

    Promise.resolve()
      .then(() => {
        if (!cancelled) {
          setIsLoading(true);
          setErrorMessage(null);
        }
        return getSubscription(accessToken, accountId);
      })
      .then((result) => {
        if (!cancelled) setSubscription(result);
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setErrorMessage(
            getErrorMessage(error, 'Could not load your subscription.'),
          );
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, accountId]);

  const activateMockPayment = useCallback(async () => {
    if (!accessToken || !accountId) {
      setErrorMessage('Please sign in again to continue.');
      return false;
    }

    setIsPaying(true);
    setErrorMessage(null);
    try {
      setSubscription(await mockPay(accessToken, accountId));
      return true;
    } catch (error) {
      setErrorMessage(
        getErrorMessage(error, 'Payment could not be completed.'),
      );
      return false;
    } finally {
      setIsPaying(false);
    }
  }, [accessToken, accountId]);

  return {
    accountId,
    displayName,
    subscription,
    isLoading,
    isPaying,
    errorMessage,
    refresh,
    activateMockPayment,
  };
}

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}
