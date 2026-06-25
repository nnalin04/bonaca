import { useCallback, useEffect, useState } from 'react';
import { Linking } from 'react-native';

import { useAuth } from '@/features/auth/AuthContext';
import { ApiError, getWearableConnection, initiateWearableConnect } from '@/lib/api';
import type { WearableConnectionResponse } from '@/types/wearable';

interface UseWearableConnectionResult {
  connection: WearableConnectionResponse | null;
  isLoading: boolean;
  isConnecting: boolean;
  errorMessage: string | null;
  connect: () => Promise<void>;
  refresh: () => Promise<void>;
}

export function useWearableConnection(memberId: string | null): UseWearableConnectionResult {
  const { accessToken } = useAuth();
  const [connection, setConnection] = useState<WearableConnectionResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isConnecting, setIsConnecting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!accessToken || !memberId) {
      setConnection(null);
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const result = await getWearableConnection(accessToken, memberId);
      setConnection(result ?? null);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setConnection(null);
      } else {
        setErrorMessage(err instanceof ApiError ? err.message : 'Could not load wearable connection.');
      }
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, memberId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const connect = useCallback(async () => {
    if (!accessToken || !memberId) return;
    setIsConnecting(true);
    setErrorMessage(null);
    try {
      const result = await initiateWearableConnect(accessToken, memberId);
      setConnection((prev) => prev ?? {
        memberId,
        spikeUserId: result.spikeUserId,
        provider: null,
        status: 'PENDING',
        connectUrl: result.connectUrl,
        connectedAt: null,
        lastSyncedAt: null,
      });
      // Open the Spike connect URL in the device browser. The user authenticates
      // with their wearable provider there; Spike calls our webhook when done.
      await Linking.openURL(result.connectUrl);
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : 'Could not start wearable connection.');
    } finally {
      setIsConnecting(false);
    }
  }, [accessToken, memberId]);

  return { connection, isLoading, isConnecting, errorMessage, connect, refresh };
}
