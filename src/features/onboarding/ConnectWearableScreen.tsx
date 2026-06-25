import { useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { PrimaryButton } from '@/features/auth/components/PrimaryButton';
import { useMembers } from '@/features/members/useMembers';
import { OnboardingHeader } from '@/features/onboarding/components/OnboardingHeader';
import { WearableOptionCard } from '@/features/onboarding/components/WearableOptionCard';
import { useWearableConnection } from '@/features/wearable/hooks/useWearableConnection';
import { Colors, Fonts } from '@/theme/tokens';

const WEARABLE_OPTIONS = [
  { label: 'Fitbit', icon: require('../../../assets/images/wearables/fitbit.png') },
  { label: 'Garmin', icon: require('../../../assets/images/wearables/garmin.png') },
  { label: 'Samsung Health', icon: require('../../../assets/images/wearables/samsung-health.png') },
  { label: 'Oura', icon: require('../../../assets/images/wearables/oura.png') },
];

export function ConnectWearableScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { self } = useMembers();
  const { connection, isConnecting, errorMessage, connect } = useWearableConnection(
    self?.id ?? null,
  );

  const goToHome = () => router.replace('/(tabs)/home');

  const isConnected = connection?.status === 'CONNECTED';
  const isPending = connection?.status === 'PENDING';

  return (
    <View style={styles.screen}>
      <OnboardingHeader title="Connect Your Wearable" onBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 16 }]}
        showsVerticalScrollIndicator={false}>
        <Text style={styles.tagline}>
          {isPending
            ? 'Complete the connection in your browser, then come back.'
            : 'Tap a device below — we\'ll open a secure page to link your account.'}
        </Text>

        {errorMessage ? <Text style={styles.error}>{errorMessage}</Text> : null}

        <View style={styles.list}>
          {WEARABLE_OPTIONS.map((option) => (
            <WearableOptionCard
              key={option.label}
              label={option.label}
              iconSource={option.icon}
              onPress={connect}
            />
          ))}
        </View>

        <View style={styles.ctaBlock}>
          {isConnecting && <ActivityIndicator color={Colors.accent} />}

          {isConnected && (
            <PrimaryButton label="Continue" onPress={goToHome} />
          )}

          {isPending && (
            <PrimaryButton label="I've Connected — Continue" onPress={goToHome} />
          )}

          <Pressable onPress={goToHome} accessibilityRole="button" accessibilityLabel="Skip for now">
            <Text style={styles.skipLink}>Skip for Now</Text>
          </Pressable>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: Colors.background },
  content: { flexGrow: 1, paddingHorizontal: 16, paddingTop: 20 },
  tagline: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textSecondary,
    marginBottom: 20,
  },
  error: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.error,
    marginBottom: 12,
  },
  list: { gap: 12 },
  ctaBlock: {
    marginTop: 'auto',
    paddingTop: 24,
    gap: 16,
    alignItems: 'center',
  },
  skipLink: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.accent,
    textDecorationLine: 'underline',
  },
});
