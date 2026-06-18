import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { SubscriptionFlowHeader } from '@/features/subscription/components/SubscriptionFlowHeader';
import { WearableProviderOption } from '@/features/subscription/components/WearableProviderOption';
import { Colors, Fonts } from '@/theme/tokens';
import type { WearableProvider } from '@/types';

// Phase 1 scope (see CLAUDE.md "Wearable Integration Phasing"): only Apple HealthKit and
// Google Health Connect are wired up via src/lib/health/HealthProvider.ts. Fitbit and Garmin
// are shown per the Figma "Select Wearable Account" list but are disabled until Phase 2
// per-vendor OAuth is explicitly scoped — selecting them is intentionally a no-op here.
const PROVIDERS: {
  provider: WearableProvider;
  label: string;
  description: string;
  available: boolean;
}[] = [
  {
    provider: 'apple-health',
    label: 'Apple Health',
    description: 'Sync steps, heart rate, sleep, and more from this device',
    available: true,
  },
  {
    provider: 'health-connect',
    label: 'Google Health Connect',
    description: 'Sync steps, heart rate, sleep, and more from this device',
    available: true,
  },
  {
    provider: 'fitbit',
    label: 'Fitbit',
    description: 'Connect a Fitbit account',
    available: false,
  },
  {
    provider: 'garmin',
    label: 'Garmin',
    description: 'Connect a Garmin account',
    available: false,
  },
];

export function SelectWearableAccountScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  const handleSelectProvider = (_provider: WearableProvider) => {
    // Pairing/connect logic is not implemented yet (src/lib/health/* are stubs by design).
    // This flow continues straight to the account's subscription/payment step, matching the
    // Figma "Connecting a Wearable" flow: Select Wearable Account → Payment Gateway.
    router.push('/subscription/payment-gateway');
  };

  return (
    <View style={styles.screen}>
      <SubscriptionFlowHeader title="Select Wearable Account" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <Text style={styles.intro}>
          Choose which wearable account to connect. You can add more later from Profile.
        </Text>

        <View style={styles.list}>
          {PROVIDERS.map((item) => (
            <WearableProviderOption
              key={item.provider}
              provider={item.provider}
              label={item.label}
              description={item.description}
              available={item.available}
              onPress={() => handleSelectProvider(item.provider)}
            />
          ))}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  content: {
    paddingHorizontal: 16,
    paddingTop: 8,
  },
  intro: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    marginBottom: 20,
  },
  list: {
    gap: 12,
  },
});
