import { useLocalSearchParams } from 'expo-router';

import { MetricDetailsScreen } from '@/features/metrics';
import type { MetricType } from '@/types';

export default function MetricDetails() {
  const { memberId, metricType } = useLocalSearchParams<{
    memberId: string;
    metricType: MetricType;
  }>();

  return <MetricDetailsScreen memberId={memberId} metricType={metricType} />;
}
