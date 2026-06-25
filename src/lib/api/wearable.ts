import { apiClient } from '@/lib/api/client';
import type { ConnectUrlResponse, WearableConnectionResponse } from '@/types/wearable';

export function initiateWearableConnect(
  accessToken: string,
  memberId: string,
): Promise<ConnectUrlResponse> {
  return apiClient.post<ConnectUrlResponse>(
    `/api/v1/members/${memberId}/wearable/connect`,
    undefined,
    accessToken,
  ) as Promise<ConnectUrlResponse>;
}

export function getWearableConnection(
  accessToken: string,
  memberId: string,
): Promise<WearableConnectionResponse | undefined> {
  return apiClient.get<WearableConnectionResponse>(
    `/api/v1/members/${memberId}/wearable/connection`,
    accessToken,
  );
}

export function disconnectWearable(
  accessToken: string,
  memberId: string,
): Promise<undefined> {
  return apiClient.delete<undefined>(
    `/api/v1/members/${memberId}/wearable/connection`,
    accessToken,
  );
}
