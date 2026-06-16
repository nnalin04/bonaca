import type { MetricReading } from '@/types';

import type { HealthProvider, TimeRange, WearableConnectionResult } from './HealthProvider';

class AppleHealthProvider implements HealthProvider {
  id = 'apple-health' as const;

  connect(): Promise<WearableConnectionResult> {
    throw new Error('Not implemented — Phase 1 wearable integration, see docs/PRD.md §8');
  }

  disconnect(): Promise<void> {
    throw new Error('Not implemented — Phase 1 wearable integration, see docs/PRD.md §8');
  }

  fetchMetrics(_range: TimeRange): Promise<MetricReading[]> {
    throw new Error('Not implemented — Phase 1 wearable integration, see docs/PRD.md §8');
  }
}

export const appleHealthProvider = new AppleHealthProvider();
