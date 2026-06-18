import type { Member, MetricReading } from '@/types';

/**
 * Mock data for Member Details (Figma node 196:4233). Standing in for the future
 * Supabase-backed member + latest-readings fetch — shape mirrors the real domain
 * types so swapping in real data later is a drop-in.
 */
export const mockMember: Member = {
  id: 'member-self',
  accountId: 'account-self',
  role: 'primary',
  name: 'Dad',
  pinned: true,
  hidden: false,
  statusMessage: 'A few vitals show improvement today',
};

const now = new Date().toISOString();

export const vitalsReadings: MetricReading[] = [
  {
    id: 'reading-heart-rate',
    memberId: mockMember.id,
    metricType: 'heart_rate',
    value: 120,
    unit: 'bpm',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'higher_than_usual',
  },
  {
    id: 'reading-hrv',
    memberId: mockMember.id,
    metricType: 'heart_rate_variability',
    value: 52,
    unit: 'ms',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'lower_than_usual',
  },
  {
    id: 'reading-spo2',
    memberId: mockMember.id,
    metricType: 'blood_oxygen',
    value: 96,
    unit: '%',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'same_as_usual',
  },
  {
    id: 'reading-respiration',
    memberId: mockMember.id,
    metricType: 'respiration_rate',
    value: 16,
    unit: 'breaths/min',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'lower_than_usual',
  },
  {
    id: 'reading-sleep',
    memberId: mockMember.id,
    metricType: 'sleep',
    value: 7.17,
    unit: 'h',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'same_as_usual',
  },
  {
    id: 'reading-stress',
    memberId: mockMember.id,
    metricType: 'stress_level',
    value: 0,
    unit: '',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'same_as_usual',
  },
  {
    id: 'reading-temperature',
    memberId: mockMember.id,
    metricType: 'body_temperature',
    value: 36.7,
    unit: '°C',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'same_as_usual',
  },
  {
    id: 'reading-ecg',
    memberId: mockMember.id,
    metricType: 'ecg',
    value: 52,
    unit: 'ms',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'same_as_usual',
  },
  {
    id: 'reading-blood-glucose',
    memberId: mockMember.id,
    metricType: 'blood_glucose',
    value: 108,
    unit: 'mg/dL',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'lower_than_usual',
  },
  {
    id: 'reading-vo2-max',
    memberId: mockMember.id,
    metricType: 'vo2_max',
    value: 42.3,
    unit: '',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'higher_than_usual',
  },
];

export const activityReadings: MetricReading[] = [
  {
    id: 'reading-steps',
    memberId: mockMember.id,
    metricType: 'steps',
    value: 8245,
    unit: 'steps',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'lower_than_usual',
  },
  {
    id: 'reading-calories',
    memberId: mockMember.id,
    metricType: 'calories',
    value: 1820,
    unit: 'calories',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'higher_than_usual',
  },
  {
    id: 'reading-workouts',
    memberId: mockMember.id,
    metricType: 'workouts',
    value: 4,
    unit: 'sessions',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'same_as_usual',
  },
  {
    id: 'reading-training-load',
    memberId: mockMember.id,
    metricType: 'training_load',
    value: 0,
    unit: '',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'same_as_usual',
  },
];

export const behaviourReadings: MetricReading[] = [
  {
    id: 'reading-routine',
    memberId: mockMember.id,
    metricType: 'routine_adherence',
    value: 80,
    unit: 'consistent',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'same_as_usual',
  },
  {
    id: 'reading-screen-time',
    memberId: mockMember.id,
    metricType: 'screen_time',
    value: 6.5,
    unit: 'h',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'higher_than_usual',
  },
  {
    id: 'reading-outdoor-time',
    memberId: mockMember.id,
    metricType: 'outdoor_time',
    value: 2.33,
    unit: 'h',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
    trendLabel: 'same_as_usual',
  },
  {
    id: 'reading-last-active-location',
    memberId: mockMember.id,
    metricType: 'last_active_location',
    value: 0,
    unit: '',
    recordedAt: now,
    sourceDeviceId: 'device-mock',
  },
];

export const sleepWeeklyBars = [
  { day: 'S', heightRatio: 0.6 },
  { day: 'M', heightRatio: 0.8 },
  { day: 'T', heightRatio: 0.5 },
  { day: 'W', heightRatio: 0.9 },
  { day: 'T', heightRatio: 0.7 },
  { day: 'F', heightRatio: 1 },
  { day: 'S', heightRatio: 0.65 },
];

/** Normalized sparkline points (0-1) for chart-style metric cards, shaped to roughly match the Figma reference curves. */
export const sparklinePoints: Partial<
  Record<MetricReading['metricType'], number[]>
> = {
  heart_rate: [0.2, 0.35, 0.3, 0.5, 0.65, 0.55, 0.8, 0.7, 0.9, 1, 0.85, 0.6],
  heart_rate_variability: [1, 0.85, 0.75, 0.6, 0.5, 0.4, 0.3, 0.25, 0.15, 0.1],
  vo2_max: [0.3, 0.4, 0.35, 0.55, 0.5, 0.7, 0.65, 0.85, 0.8, 1],
};
