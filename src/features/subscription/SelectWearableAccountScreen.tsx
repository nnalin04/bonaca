import { useRouter } from 'expo-router';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useMembers } from '@/features/members/useMembers';
import { ConnectionErrorToast } from '@/features/subscription/components/ConnectionErrorToast';
import { SubscriptionHeader } from '@/features/subscription/components/SubscriptionHeader';
import { WearableProviderRow } from '@/features/subscription/components/WearableProviderRow';
import { useWearableConnection } from '@/features/wearable/hooks/useWearableConnection';
import { Colors, Fonts } from '@/theme/tokens';
import type { WearableProvider } from '@/types';

export type SelectWearableAccountVariant = 'initial' | 'mid-flow' | 'retry';

interface ProviderOption {
  provider: WearableProvider;
  label: string;
  iconSource: number;
}

const providerOptions: ProviderOption[] = [
  { provider: 'fitbit', label: 'Fitbit', iconSource: require('../../../assets/images/wearables/fitbit.png') },
  { provider: 'garmin', label: 'Garmin', iconSource: require('../../../assets/images/wearables/garmin.png') },
  { provider: 'samsung-health', label: 'Samsung Health', iconSource: require('../../../assets/images/wearables/samsung-health.png') },
  { provider: 'oura', label: 'Oura', iconSource: require('../../../assets/images/wearables/oura.png') },
];

interface SelectWearableAccountScreenProps {
  variant?: SelectWearableAccountVariant;
}

export function SelectWearableAccountScreen({ variant = 'mid-flow' }: SelectWearableAccountScreenProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { self } = useMembers();
  const { connection, isConnecting, errorMessage, connect } = useWearableConnection(self?.id ?? null);

  const showBack = variant !== 'initial';
  const showErrorToast = variant === 'retry' || !!errorMessage;
  const isConnected = connection?.status === 'CONNECTED';
  const isPending = connection?.status === 'PENDING';

  const handleSelectProvider = (_provider: WearableProvider) => {
    connect();
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

        <Text style={styles.intro}>
          {isPending
            ? 'Complete the connection in your browser, then come back here.'
            : isConnected
            ? 'Your wearable is connected. Data will start syncing shortly.'
            : 'Link a wearable account to track health and activity'}
        </Text>

        {isConnecting && (
          <View style={styles.spinnerRow}>
            <ActivityIndicator color={Colors.accent} />
            <Text style={styles.spinnerText}>Opening connection page…</Text>
          </View>
        )}

        <View style={styles.list}>
          {providerOptions.map((option) => (
            <WearableProviderRow
              key={option.provider}
              provider={option.provider}
              label={option.label}
              iconSource={option.iconSource}
              onPress={() => handleSelectProvider(option.provider)}
            />
          ))}
        </View>

        <View style={styles.footer}>
          {showErrorToast && (
            <ConnectionErrorToast message={errorMessage ?? 'Failed to connect, please try again'} />
          )}

          <Text
            style={styles.connectLater}
            accessibilityRole="link"
            onPress={() => router.push('/subscription/payment-gateway')}>
            {isPending ? "I've Connected — Continue" : "I’ll Connect Later"}
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: Colors.background },
  content: { paddingHorizontal: 16, paddingTop: 20, flexGrow: 1 },
  intro: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textSecondary,
  },
  spinnerRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 16 },
  spinnerText: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    color: Colors.textSecondary,
  },
  list: { marginTop: 20, gap: 12 },
  footer: {
    flex: 1,
    justifyContent: 'flex-end',
    alignItems: 'center',
    gap: 16,
    paddingTop: 24,
  },
  connectLater: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.accent,
    textDecorationLine: 'underline',
  },
});
