import {
  IconArticle,
  IconCash,
  IconFileDescription,
  IconFileTextShield,
  IconListCheck,
  IconLogout,
  IconUsers,
} from '@tabler/icons-react-native';
import { useRouter, type Href } from 'expo-router';
import type { ComponentType } from 'react';
import { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { useMembers } from '@/features/members';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { ProfileSummaryCard } from '@/features/profile/components/ProfileSummaryCard';
import { SettingsListItem } from '@/features/profile/components/SettingsListItem';
import { WearableConnectCard } from '@/features/profile/components/WearableConnectCard';
import { WearableConnectedCard } from '@/features/profile/components/WearableConnectedCard';
import { getMe } from '@/lib/api';
import { Colors } from '@/theme/tokens';
import type { WearableConnection } from '@/types';

const wearableConnection: WearableConnection | null = null;

const providerLabels: Record<WearableConnection['provider'], string> = {
  'apple-health': 'Apple Health',
  'health-connect': 'Health Connect',
  fitbit: 'Fitbit',
  garmin: 'Garmin',
  'samsung-health': 'Samsung Health',
  oura: 'Oura',
};

interface SettingsRowConfig {
  key: string;
  icon: ComponentType<{ size?: number; color?: string; strokeWidth?: number }>;
  label: string;
  onPress: () => void;
}

export function ProfileScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { accessToken } = useAuth();
  const { self } = useMembers();
  const [phoneNumber, setPhoneNumber] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!accessToken) return;
    getMe(accessToken).then((result) => {
      if (!cancelled) setPhoneNumber(result.phoneNumber);
    });
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  const settingsRows: SettingsRowConfig[] = [
    {
      key: 'members',
      icon: IconListCheck,
      label: 'Members & Permissions',
      onPress: () => router.push('/members'),
    },
    {
      key: 'subscription',
      icon: IconCash,
      label: 'Manage Subscription',
      onPress: () => router.push('/subscription' as Href),
    },
    {
      key: 'hidden-members',
      icon: IconUsers,
      label: 'Hidden Members',
      onPress: () => router.push('/members/hidden'),
    },
    {
      key: 'documentation',
      icon: IconFileDescription,
      label: 'Documentation',
      onPress: () => {},
    },
    { key: 'terms', icon: IconArticle, label: 'Terms & Conditions', onPress: () => {} },
    {
      key: 'privacy',
      icon: IconFileTextShield,
      label: 'Privacy Policy',
      onPress: () => {},
    },
    {
      key: 'logout',
      icon: IconLogout,
      label: 'Log Out',
      onPress: () => router.replace('/(auth)/login'),
    },
  ];

  return (
    <View style={styles.screen}>
      <ProfileHeader title="Profile & Settings" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <ProfileSummaryCard
          avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
          name={self?.nickname ?? self?.name ?? ''}
          phoneNumber={phoneNumber ?? ''}
          onPress={() => router.push('/profile/details' as Href)}
        />

        <View style={styles.wearableSection}>
          {wearableConnection ? (
            <WearableConnectedCard
              providerLabel={providerLabels[wearableConnection.provider]}
              syncLabel={
                wearableConnection.lastSyncedAt ? 'Last synced: Just now' : 'Not yet synced'
              }
              onPressDisconnect={() => {}}
            />
          ) : (
            <WearableConnectCard onPressConnect={() => router.push('/(auth)/connect-wearable')} />
          )}
        </View>

        <View style={styles.settingsList}>
          {settingsRows.map((row) => (
            <SettingsListItem
              key={row.key}
              icon={row.icon}
              label={row.label}
              onPress={row.onPress}
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
    paddingTop: 20,
  },
  wearableSection: {
    marginTop: 24,
  },
  settingsList: {
    marginTop: 24,
    gap: 12,
  },
});
