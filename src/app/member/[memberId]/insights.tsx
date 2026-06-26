import { useLocalSearchParams } from 'expo-router';

import { InsightsFeedScreen } from '@/features/insights/InsightsFeedScreen';

export default function InsightsRoute() {
  const { memberId } = useLocalSearchParams<{ memberId: string }>();
  return <InsightsFeedScreen memberId={memberId} />;
}
