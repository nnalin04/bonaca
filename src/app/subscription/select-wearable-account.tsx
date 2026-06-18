import { useLocalSearchParams } from 'expo-router';

import { SelectWearableAccountScreen } from '@/features/subscription';
import type { SelectWearableAccountVariant } from '@/features/subscription';

const validVariants: SelectWearableAccountVariant[] = ['initial', 'mid-flow', 'retry'];

export default function SelectWearableAccountRoute() {
  const { variant } = useLocalSearchParams<{ variant?: string }>();
  const resolvedVariant = validVariants.includes(variant as SelectWearableAccountVariant)
    ? (variant as SelectWearableAccountVariant)
    : 'mid-flow';

  return <SelectWearableAccountScreen variant={resolvedVariant} />;
}
