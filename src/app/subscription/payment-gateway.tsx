import { useLocalSearchParams } from 'expo-router';

import { PaymentGatewayScreen } from '@/features/subscription';

export default function PaymentGatewayRoute() {
  const { accountId } = useLocalSearchParams<{ accountId?: string }>();
  return <PaymentGatewayScreen accountId={accountId} />;
}
