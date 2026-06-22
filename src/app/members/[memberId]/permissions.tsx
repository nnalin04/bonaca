import { useLocalSearchParams } from 'expo-router';

import { EditPermissionsScreen } from '@/features/members';

export default function EditPermissionsRoute() {
  const { memberId } = useLocalSearchParams<{ memberId: string }>();
  return <EditPermissionsScreen memberId={memberId} />;
}
