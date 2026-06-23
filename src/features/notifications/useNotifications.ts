import { useCallback, useEffect, useState } from 'react';

import { useAuth } from '@/features/auth/AuthContext';
import { ApiError, getNotifications, markNotificationRead } from '@/lib/api';
import type { NotificationResponse } from '@/types/notifications';

interface UseNotificationsResult {
  notifications: NotificationResponse[];
  isLoading: boolean;
  errorMessage: string | null;
  markRead: (notificationId: string) => Promise<void>;
  refresh: () => Promise<void>;
}

/** memberId is the requesting user's own Member id — notifications are personal, not account-shared (see backend plan doc §7). */
export function useNotifications(
  memberId: string | undefined,
): UseNotificationsResult {
  const { accessToken } = useAuth();
  const [notifications, setNotifications] = useState<NotificationResponse[]>(
    [],
  );
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!accessToken || !memberId) {
      setNotifications([]);
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setErrorMessage(null);
    try {
      setNotifications(await getNotifications(accessToken, memberId));
    } catch (error) {
      setErrorMessage(
        error instanceof ApiError
          ? error.message
          : 'Could not load your notifications.',
      );
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, memberId]);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken || !memberId) {
      Promise.resolve().then(() => {
        if (!cancelled) {
          setNotifications([]);
          setIsLoading(false);
        }
      });
      return () => {
        cancelled = true;
      };
    }
    getNotifications(accessToken, memberId)
      .then((result) => {
        if (!cancelled) setNotifications(result);
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setErrorMessage(
            error instanceof ApiError
              ? error.message
              : 'Could not load your notifications.',
          );
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, memberId]);

  const markRead = useCallback(
    async (notificationId: string) => {
      if (!accessToken) return;
      const updated = await markNotificationRead(accessToken, notificationId);
      setNotifications((current) =>
        current.map((n) => (n.id === updated.id ? updated : n)),
      );
    },
    [accessToken],
  );

  return { notifications, isLoading, errorMessage, markRead, refresh };
}
