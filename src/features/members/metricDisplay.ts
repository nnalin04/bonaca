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
  iconColor: string;
  formatValue: (value: number) => string;
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
    iconColor: '#e07a5f',
    formatValue: (v) => `${v}`,
    unitSuffix: 'bpm',
  },
  heart_rate_variability: {
    label: 'HRV',
    icon: IconWaveSine,
    iconColor: '#3a7f7c',
    formatValue: (v) => `${v}`,
    unitSuffix: 'ms',
  },
  blood_oxygen: {
    label: 'SpO2',
    icon: IconDroplet,
    iconColor: '#5b8def',
    formatValue: (v) => `${v}`,
    unitSuffix: '%',
  },
  respiration_rate: {
    label: 'Respiration',
    icon: IconLungs,
    iconColor: '#6c8ea3',
    formatValue: (v) => `${v}`,
    unitSuffix: 'breaths/min',
  },
  sleep: {
    label: 'Sleep',
    icon: IconMoonStars,
    iconColor: '#5e5a8a',
    formatValue: formatHours,
    unitSuffix: '',
  },
  stress_level: {
    label: 'Stress',
    icon: IconSeedling,
    iconColor: '#6bae92',
    formatValue: () => 'Moderate',
    unitSuffix: '',
  },
  body_temperature: {
    label: 'Temperature',
    icon: IconTemperature,
    iconColor: '#bbbbbb',
    formatValue: (v) => `${v}`,
    unitSuffix: '°C',
  },
  ecg: {
    label: 'ECG',
    icon: IconHeartbeat,
    iconColor: '#e07a5f',
    formatValue: (v) => `${v}`,
    unitSuffix: 'ms',
  },
  blood_glucose: {
    label: 'Blood Glucose',
    icon: IconDropletHeart,
    iconColor: '#8b6f9c',
    formatValue: (v) => `${v}`,
    unitSuffix: 'mg/dL',
  },
  vo2_max: {
    label: 'VO₂ Max',
    icon: IconTreadmill,
    iconColor: '#d4a24c',
    formatValue: (v) => `${v}`,
    unitSuffix: '',
  },
  steps: {
    label: 'Steps',
    icon: IconWalk,
    iconColor: '#5b8def',
    formatValue: (v) => v.toLocaleString('en-US'),
    unitSuffix: 'steps',
  },
  calories: {
    label: 'Calories',
    icon: IconFlame,
    iconColor: '#e07a5f',
    formatValue: (v) => v.toLocaleString('en-US'),
    unitSuffix: 'calories',
  },
  workouts: {
    label: 'Workouts',
    icon: IconBarbell,
    iconColor: '#d4a24c',
    formatValue: (v) => `${v}`,
    unitSuffix: 'sessions',
  },
  training_load: {
    label: 'Training Load',
    icon: IconWeight,
    iconColor: '#3a7f7c',
    formatValue: () => 'Moderate',
    unitSuffix: '',
  },
  routine_adherence: {
    label: 'Routine',
    icon: IconActivity,
    iconColor: '#5b8def',
    formatValue: (v) => `${v}%`,
    unitSuffix: 'consistent',
  },
  screen_time: {
    label: 'Screen Time',
    icon: IconDeviceMobile,
    iconColor: '#e07a5f',
    formatValue: formatHours,
    unitSuffix: '',
  },
  outdoor_time: {
    label: 'Outdoor Time',
    icon: IconTrees,
    iconColor: '#3a7f7c',
    formatValue: formatHours,
    unitSuffix: '',
  },
  last_active_location: {
    label: 'Last Active Location',
    icon: IconUserPin,
    iconColor: '#5e5a8a',
    formatValue: () => 'HSR Layout, Bengaluru, K’taka',
    unitSuffix: '',
  },
};
