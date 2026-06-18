import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { PrimaryButton } from '@/features/auth/components/PrimaryButton';
import { OnboardingHeader } from '@/features/onboarding/components/OnboardingHeader';
import { WearableOptionCard } from '@/features/onboarding/components/WearableOptionCard';
import { Colors, Fonts } from '@/theme/tokens';

interface WearableOption {
  label: string;
  icon: number;
}

// Figma node 60:634 lists these four vendors. Per CLAUDE.md, Phase 1 scope is
// Apple HealthKit + Google Health Connect only (on-device, no remote read) —
// Fitbit/Garmin/Samsung Health/Oura are Phase 2 (separate OAuth scoping, not started)
// and aren't even represented in the WearableProvider domain type yet. Rendered here
// to stay pixel-faithful to the design; selecting an option does not wire up to any
// real wearable SDK — see open question in the build report.
const WEARABLE_OPTIONS: WearableOption[] = [
  { label: 'Fitbit', icon: require('../../../assets/images/wearables/fitbit.png') },
  { label: 'Garmin', icon: require('../../../assets/images/wearables/garmin.png') },
  { label: 'Samsung Health', icon: require('../../../assets/images/wearables/samsung-health.png') },
  { label: 'Oura', icon: require('../../../assets/images/wearables/oura.png') },
];

export function ConnectWearableScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  // Figma 60:634 ships with the "Continue" CTA hidden (visible: false in the source) —
  // only "Skip For Now" shows until a wearable has actually been linked. No "connected"
  // state screenshot was cached, so we approximate: selecting any option reveals Continue.
  const [hasSelection, setHasSelection] = useState(false);

  const goToHome = () => router.replace('/(tabs)/home');

  return (
    <View style={styles.screen}>
      <OnboardingHeader title="Connect Your Wearable" onBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 16 }]}
        showsVerticalScrollIndicator={false}>
        <Text style={styles.tagline}>Link a wearable account to track health and activity</Text>

        <View style={styles.list}>
          {WEARABLE_OPTIONS.map((option) => (
            <WearableOptionCard
              key={option.label}
              label={option.label}
              iconSource={option.icon}
              onPress={() => setHasSelection(true)}
            />
          ))}
        </View>

        <View style={styles.ctaBlock}>
          {hasSelection && <PrimaryButton label="Continue" onPress={goToHome} />}
          <Pressable onPress={goToHome} accessibilityRole="button" accessibilityLabel="Skip for now">
            <Text style={styles.skipLink}>Skip For Now</Text>
          </Pressable>
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
    flexGrow: 1,
    paddingHorizontal: 16,
    paddingTop: 24,
  },
  tagline: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textSecondary,
    marginBottom: 24,
  },
  list: {
    gap: 12,
  },
  ctaBlock: {
    marginTop: 'auto',
    paddingTop: 24,
    gap: 24,
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
