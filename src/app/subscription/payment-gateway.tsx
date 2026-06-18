import { useLocalSearchParams } from 'expo-router';

import { PaymentGatewayScreen } from '@/features/subscription';
import type { PaymentGatewayVariant } from '@/features/subscription';

const validVariants: PaymentGatewayVariant[] = ['trial-signup', 'renewal'];

export default function PaymentGatewayRoute() {
  const { variant } = useLocalSearchParams<{ variant?: string }>();
  const resolvedVariant = validVariants.includes(variant as PaymentGatewayVariant)
    ? (variant as PaymentGatewayVariant)
    : 'trial-signup';

  return <PaymentGatewayScreen variant={resolvedVariant} />;
}
