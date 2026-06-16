import type { MetricReading, WearableConnection, WearableProvider } from '@/types';

export interface TimeRange {
  start: string;
  end: string;
}

export interface WearableConnectionResult {
  connection: WearableConnection;
}

export interface HealthProvider {
  id: WearableProvider;
  connect(): Promise<WearableConnectionResult>;
  disconnect(): Promise<void>;
  fetchMetrics(range: TimeRange): Promise<MetricReading[]>;
}
