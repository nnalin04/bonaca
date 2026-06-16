import { useLocalSearchParams } from 'expo-router';

import { ScreenPlaceholder } from '@/components/ScreenPlaceholder';

export default function MemberDetailsScreen() {
  const { memberId } = useLocalSearchParams<{ memberId: string }>();
  return (
    <ScreenPlaceholder
      title={`Member Details — ${memberId}`}
      figmaSection="Member Details (Vitals / Activity / Behaviour tabs)"
    />
  );
}
