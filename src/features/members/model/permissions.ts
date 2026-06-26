import type { SharingScope } from '@/types';

export const sharingScopes: SharingScope[] = [
  'vitals',
  'activity',
  'behaviour',
];

export const sharingScopeLabels: Record<SharingScope, string> = {
  vitals: 'Vitals',
  activity: 'Activity',
  behaviour: 'Behaviour',
};

export const sharingScopeMetrics: Record<SharingScope, string[]> = {
  vitals: [
    'Heart Rate',
    'HRV',
    'Stress',
    'SpO2',
    'Respiration',
    'Sleep',
    'ECG',
    'VO2 max',
  ],
  activity: ['Steps', 'Calories', 'Workouts', 'Training Load'],
  behaviour: ['Routine Adherence', 'Screen Time', 'Outside Time'],
};
