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
import { Linking, ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '@/features/auth/AuthContext';
import { useMembers } from '@/features/members';
import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { ProfileSummaryCard } from '@/features/profile/components/ProfileSummaryCard';
import { SettingsListItem } from '@/features/profile/components/SettingsListItem';
import { WearableConnectCard } from '@/features/profile/components/WearableConnectCard';
import { WearableConnectedCard } from '@/features/profile/components/WearableConnectedCard';
import { useProfileSummary } from '@/features/profile/hooks/useProfileSummary';
import { useWearableConnection } from '@/features/wearable/hooks/useWearableConnection';
import { Colors } from '@/theme/tokens';

const providerLabels: Record<string, string> = {
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
  const { logout } = useAuth();
  const { self } = useMembers();
  const { phoneNumber } = useProfileSummary();
  const { connection: wearableConn, disconnect } = useWearableConnection(self?.id ?? null);
  const isConnected = wearableConn?.status === 'CONNECTED';

  const handleLogout = async () => {
    await logout();
    router.replace('/(auth)/login');
  };

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
      onPress: () => Linking.openURL('https://bonaca.in/docs'),
    },
    {
      key: 'terms',
      icon: IconArticle,
      label: 'Terms & Conditions',
      onPress: () => Linking.openURL('https://bonaca.in/terms'),
    },
    {
      key: 'privacy',
      icon: IconFileTextShield,
      label: 'Privacy Policy',
      onPress: () => Linking.openURL('https://bonaca.in/privacy'),
    },
    {
      key: 'logout',
      icon: IconLogout,
      label: 'Log Out',
      onPress: () => void handleLogout(),
    },
  ];

  return (
    <View style={styles.screen}>
      <ProfileHeader
        title="Profile & Settings"
        onPressBack={() => router.back()}
      />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <ProfileSummaryCard
          avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
          name={self?.nickname ?? self?.name ?? ''}
          phoneNumber={phoneNumber ?? ''}
          onPress={() => router.push('/profile/details' as Href)}
        />

        <View style={styles.wearableSection}>
          {isConnected ? (
            <WearableConnectedCard
              providerLabel={
                wearableConn?.provider
                  ? (providerLabels[wearableConn.provider] ?? wearableConn.provider)
                  : 'Wearable'
              }
              syncLabel={
                wearableConn?.lastSyncedAt
                  ? 'Last synced: Just now'
                  : 'Not yet synced'
              }
              onPressDisconnect={() => void disconnect()}
            />
          ) : (
            <WearableConnectCard
              onPressConnect={() =>
                router.push('/subscription/select-wearable-account' as Href)
              }
            />
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
