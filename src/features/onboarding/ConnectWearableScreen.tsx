import { IconActivity, IconDeviceWatch, IconHeartbeat, IconMapPin } from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { WearablePermissionRow } from '@/features/onboarding/components/WearablePermissionRow';
import { getPlatformHealthProvider } from '@/lib/health';
import { Colors, Fonts, Radii } from '@/theme/tokens';

const PROVIDER_LABEL: Record<string, string> = {
  'apple-health': 'Apple Health',
  'health-connect': 'Health Connect',
};

export function ConnectWearableScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [isConnecting, setIsConnecting] = useState(false);

  const provider = getPlatformHealthProvider();
  const providerLabel = PROVIDER_LABEL[provider.id] ?? (Platform.OS === 'ios' ? 'Apple Health' : 'Health Connect');

  const handleConnect = async () => {
    setIsConnecting(true);
    try {
      await provider.connect();
    } catch {
      // Phase 1 wearable integration is not implemented yet (src/lib/health/* are stubs
      // per CLAUDE.md) — proceed to Home regardless so the onboarding flow stays usable.
    } finally {
      setIsConnecting(false);
      router.replace('/(tabs)/home');
    }
  };

  const handleSkip = () => {
    router.replace('/(tabs)/home');
  };

  return (
    <View style={styles.screen}>
      <View style={[styles.content, { paddingTop: insets.top + 32, paddingBottom: insets.bottom + 24 }]}>
        <View style={styles.iconBadge}>
          <IconDeviceWatch size={40} color={Colors.accent} strokeWidth={1.75} />
        </View>

        <Text style={styles.title}>Connect your wearable</Text>
        <Text style={styles.subtitle}>
          Link {providerLabel} so your family can see your vitals and activity, kept up to date
          automatically.
        </Text>

        <View style={styles.permissionsCard}>
          <WearablePermissionRow
            icon={IconHeartbeat}
            title="Vitals"
            description="Heart rate and other key health signals"
          />
          <View style={styles.divider} />
          <WearablePermissionRow
            icon={IconActivity}
            title="Activity"
            description="Steps, sleep, and daily routine"
          />
          <View style={styles.divider} />
          <WearablePermissionRow
            icon={IconMapPin}
            title="Location"
            description="Last active location, only if you choose to share it"
          />
        </View>

        <View style={styles.spacer} />

        <Pressable
          style={[styles.cta, isConnecting && styles.ctaDisabled]}
          onPress={handleConnect}
          disabled={isConnecting}
          accessibilityRole="button"
          accessibilityLabel={`Connect ${providerLabel}`}>
          {isConnecting ? (
            <ActivityIndicator color={Colors.white} />
          ) : (
            <Text style={styles.ctaText}>Connect {providerLabel}</Text>
          )}
        </Pressable>

        <Pressable
          style={styles.skipButton}
          onPress={handleSkip}
          accessibilityRole="button"
          accessibilityLabel="Skip for now">
          <Text style={styles.skipText}>Skip for now</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  content: {
    flex: 1,
    paddingHorizontal: 24,
  },
  iconBadge: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: Colors.white,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
  },
  title: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 24,
    lineHeight: 32,
    color: Colors.textPrimary,
    marginBottom: 8,
  },
  subtitle: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 14,
    lineHeight: 20,
    color: Colors.textSecondary,
    marginBottom: 32,
  },
  permissionsCard: {
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    padding: 16,
    gap: 16,
  },
  divider: {
    height: 1,
    backgroundColor: Colors.cardBorder,
  },
  spacer: {
    flex: 1,
  },
  cta: {
    height: 56,
    borderRadius: Radii.pill,
    backgroundColor: Colors.headerGradientEnd,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  ctaDisabled: {
    opacity: 0.7,
  },
  ctaText: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    color: Colors.white,
  },
  skipButton: {
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  skipText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 14,
    color: Colors.textSecondary,
  },
});
