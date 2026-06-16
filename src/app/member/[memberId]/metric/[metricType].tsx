import { useLocalSearchParams } from 'expo-router';

import { ScreenPlaceholder } from '@/components/ScreenPlaceholder';

export default function MetricDetailsScreen() {
  const { memberId, metricType } = useLocalSearchParams<{
    memberId: string;
    metricType: string;
  }>();
  return (
    <ScreenPlaceholder
      title={`${metricType} — ${memberId}`}
      figmaSection="Metric Details"
    />
  );
}
