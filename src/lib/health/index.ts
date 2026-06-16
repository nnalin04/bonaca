import { Platform } from 'react-native';

import { appleHealthProvider } from './appleHealth';
import { healthConnectProvider } from './healthConnect';
import type { HealthProvider } from './HealthProvider';

export function getPlatformHealthProvider(): HealthProvider {
  return Platform.OS === 'ios' ? appleHealthProvider : healthConnectProvider;
}

export type { HealthProvider, TimeRange, WearableConnectionResult } from './HealthProvider';
