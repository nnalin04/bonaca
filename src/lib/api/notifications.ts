import { apiClient } from '@/lib/api/client';
import type { NotificationResponse } from '@/types/notifications';

export function getNotifications(accessToken: string, memberId: string): Promise<NotificationResponse[]> {
  return apiClient.get<NotificationResponse[]>(
    `/api/v1/members/${memberId}/notifications`,
    accessToken
  ) as Promise<NotificationResponse[]>;
}

export function markNotificationRead(accessToken: string, notificationId: string): Promise<NotificationResponse> {
  return apiClient.patch<NotificationResponse>(
    `/api/v1/notifications/${notificationId}/read`,
    undefined,
    accessToken
  ) as Promise<NotificationResponse>;
}
