import { useLocalSearchParams } from 'expo-router';

import { MemberDetailsScreen } from '@/features/members';

export default function MemberDetails() {
  const { memberId } = useLocalSearchParams<{ memberId: string }>();
  return <MemberDetailsScreen memberId={memberId} />;
}
