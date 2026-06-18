import {
  IconActivity,
  IconBarbell,
  IconDeviceMobile,
  IconDroplet,
  IconDropletHeart,
  IconFlame,
  IconHeart,
  IconHeartbeat,
  IconLungs,
  IconMoonStars,
  IconSeedling,
  IconTemperature,
  IconTreadmill,
  IconTrees,
  IconUserPin,
  IconWalk,
  IconWaveSine,
  IconWeight,
  type Icon,
} from '@tabler/icons-react-native';

import type { MetricTrendLabel, MetricType } from '@/types';

export interface MetricDisplayConfig {
  label: string;
  icon: Icon;
  /** Formats a raw MetricReading.value into the string shown as the headline figure (e.g. "7h 10m", "120"). */
  formatValue: (value: number) => string;
  /** Unit suffix shown next to the headline figure (e.g. "bpm"). Empty string when the formatted value is self-contained (e.g. "Moderate", "7h 10m"). */
  unitSuffix: string;
}

const trendLabelText: Record<MetricTrendLabel, string> = {
  higher_than_usual: 'Higher than usual',
  lower_than_usual: 'Lower than usual',
  same_as_usual: 'Same as usual',
};

export function formatTrendLabel(
  trendLabel: MetricTrendLabel | undefined,
): string | null {
  return trendLabel ? trendLabelText[trendLabel] : null;
}

function formatHours(value: number): string {
  const hours = Math.floor(value);
  const minutes = Math.round((value - hours) * 60);
  return minutes > 0 ? `${hours}h ${minutes}m` : `${hours}h`;
}

export const metricDisplayConfig: Record<MetricType, MetricDisplayConfig> = {
  heart_rate: {
    label: 'Heart Rate',
    icon: IconHeart,
    formatValue: (v) => `${v}`,
    unitSuffix: 'bpm',
  },
  heart_rate_variability: {
    label: 'HRV',
    icon: IconWaveSine,
    formatValue: (v) => `${v}`,
    unitSuffix: 'ms',
  },
  blood_oxygen: {
    label: 'SpO2',
    icon: IconDroplet,
    formatValue: (v) => `${v}`,
    unitSuffix: '%',
  },
  respiration_rate: {
    label: 'Respiration',
    icon: IconLungs,
    formatValue: (v) => `${v}`,
    unitSuffix: 'breaths/min',
  },
  sleep: {
    label: 'Sleep',
    icon: IconMoonStars,
    formatValue: formatHours,
    unitSuffix: '',
  },
  stress_level: {
    label: 'Stress',
    icon: IconSeedling,
    formatValue: () => 'Moderate',
    unitSuffix: '',
  },
  body_temperature: {
    label: 'Temperature',
    icon: IconTemperature,
    formatValue: (v) => `${v}`,
    unitSuffix: '°C',
  },
  ecg: {
    label: 'ECG',
    icon: IconHeartbeat,
    formatValue: (v) => `${v}`,
    unitSuffix: 'ms',
  },
  blood_glucose: {
    label: 'Blood Glucose',
    icon: IconDropletHeart,
    formatValue: (v) => `${v}`,
    unitSuffix: 'mg/dL',
  },
  vo2_max: {
    label: 'VO₂ Max',
    icon: IconTreadmill,
    formatValue: (v) => `${v}`,
    unitSuffix: '',
  },
  steps: {
    label: 'Steps',
    icon: IconWalk,
    formatValue: (v) => v.toLocaleString('en-US'),
    unitSuffix: 'steps',
  },
  calories: {
    label: 'Calories',
    icon: IconFlame,
    formatValue: (v) => v.toLocaleString('en-US'),
    unitSuffix: 'calories',
  },
  workouts: {
    label: 'Workouts',
    icon: IconBarbell,
    formatValue: (v) => `${v}`,
    unitSuffix: 'sessions',
  },
  training_load: {
    label: 'Training Load',
    icon: IconWeight,
    formatValue: () => 'Moderate',
    unitSuffix: '',
  },
  routine_adherence: {
    label: 'Routine',
    icon: IconActivity,
    formatValue: (v) => `${v}%`,
    unitSuffix: 'consistent',
  },
  screen_time: {
    label: 'Screen Time',
    icon: IconDeviceMobile,
    formatValue: formatHours,
    unitSuffix: '',
  },
  outdoor_time: {
    label: 'Outdoor Time',
    icon: IconTrees,
    formatValue: formatHours,
    unitSuffix: '',
  },
  last_active_location: {
    label: 'Last Active Location',
    icon: IconUserPin,
    formatValue: () => 'HSR Layout, Bengaluru, K’taka',
    unitSuffix: '',
  },
};
