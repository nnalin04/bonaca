export type WearableConnectionStatus = 'PENDING' | 'CONNECTED' | 'DISCONNECTED';

export interface WearableConnectionResponse {
  memberId: string;
  spikeUserId: string;
  provider: string | null;
  status: WearableConnectionStatus;
  connectUrl: string | null;
  connectedAt: string | null;
  lastSyncedAt: string | null;
}

export interface ConnectUrlResponse {
  connectUrl: string;
  spikeUserId: string;
}
