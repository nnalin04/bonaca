import { IconActivity, IconCircleDot, IconDeviceWatch } from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ConnectionErrorToast } from '@/features/subscription/components/ConnectionErrorToast';
import { SubscriptionHeader } from '@/features/subscription/components/SubscriptionHeader';
import { WearableProviderRow } from '@/features/subscription/components/WearableProviderRow';
import { Colors, Fonts } from '@/theme/tokens';
import type { WearableProvider } from '@/types';

export type SelectWearableAccountVariant = 'initial' | 'mid-flow' | 'retry';

interface ProviderOption {
  provider: WearableProvider;
  label: string;
  iconBackground: string;
}

const providerOptions: ProviderOption[] = [
  { provider: 'fitbit', label: 'Fitbit', iconBackground: '#0b3b36' },
  { provider: 'garmin', label: 'Garmin', iconBackground: '#000000' },
  { provider: 'samsung-health', label: 'Samsung Health', iconBackground: '#1ba9b5' },
  { provider: 'oura', label: 'Oura', iconBackground: '#16203c' },
];

function providerIcon(provider: WearableProvider) {
  switch (provider) {
    case 'samsung-health':
      return <IconActivity size={18} color={Colors.white} strokeWidth={2} />;
    case 'oura':
      return <IconCircleDot size={18} color={Colors.white} strokeWidth={2} />;
    default:
      return <IconDeviceWatch size={18} color={Colors.white} strokeWidth={2} />;
  }
}

interface SelectWearableAccountScreenProps {
  variant?: SelectWearableAccountVariant;
}

export function SelectWearableAccountScreen({
  variant = 'mid-flow',
}: SelectWearableAccountScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const showBack = variant !== 'initial';
  const showErrorToast = variant === 'retry';

  const handleSelectProvider = (_provider: WearableProvider) => {
    router.push('/subscription/payment-gateway');
  };

  return (
    <View style={styles.screen}>
      <SubscriptionHeader
        title="Connect Your Wearable"
        onPressBack={showBack ? () => router.back() : undefined}
      />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <Text style={styles.intro}>Link a wearable account to track health and activity</Text>

        <View style={styles.list}>
          {providerOptions.map((option) => (
            <WearableProviderRow
              key={option.provider}
              provider={option.provider}
              label={option.label}
              iconBackground={option.iconBackground}
              icon={providerIcon(option.provider)}
              onPress={() => handleSelectProvider(option.provider)}
            />
          ))}
        </View>

        <View style={styles.footer}>
          {showErrorToast && (
            <ConnectionErrorToast message="Failed to connect, please try again" />
          )}

          <Text
            style={styles.connectLater}
            accessibilityRole="link"
            onPress={() => router.push('/subscription/payment-gateway')}>
            I&rsquo;ll Connect Later
          </Text>
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
    paddingTop: 20,
    flexGrow: 1,
  },
  intro: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textSecondary,
  },
  list: {
    marginTop: 24,
    gap: 12,
  },
  footer: {
    flex: 1,
    justifyContent: 'flex-end',
    alignItems: 'center',
    gap: 16,
    paddingTop: 24,
  },
  connectLater: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    color: Colors.accent,
    textDecorationLine: 'underline',
  },
});
