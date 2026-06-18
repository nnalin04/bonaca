import {
  IconArticle,
  IconCash,
  IconFileDescription,
  IconFileTextShield,
  IconListCheck,
  IconLogout,
  IconUsers,
} from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ProfileHeader } from '@/features/profile/components/ProfileHeader';
import { ProfileSummaryCard } from '@/features/profile/components/ProfileSummaryCard';
import { SettingsListCard } from '@/features/profile/components/SettingsListCard';
import { SettingsListItem } from '@/features/profile/components/SettingsListItem';
import { WearableConnectCard } from '@/features/profile/components/WearableConnectCard';
import { WearableConnectedCard } from '@/features/profile/components/WearableConnectedCard';
import { Colors } from '@/theme/tokens';
import type { Member, WearableConnection } from '@/types';

// Mock "current user" — mirrors the local hardcoded pattern used in HomeScreen.tsx.
// Swap `role` to 'secondary' to preview the Secondary Member variant during dev.
const currentMember: Member = {
  id: 'member-self',
  accountId: 'account-self',
  role: 'primary',
  name: 'Prasanna Kumar',
  pinned: true,
  hidden: false,
};

const currentMemberPhone = '+91 97426 59964';

const wearableConnection: WearableConnection | null = null;
// Example of a connected state, used when wearableConnection is non-null:
// { id: 'w1', memberId: 'member-self', provider: 'fitbit', status: 'connected', lastSyncedAt: new Date().toISOString() };

const providerLabels: Record<WearableConnection['provider'], string> = {
  'apple-health': 'Apple Health',
  'health-connect': 'Health Connect',
  fitbit: 'Fitbit',
  garmin: 'Garmin',
};

export function ProfileScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const isPrimary = currentMember.role === 'primary';

  return (
    <View style={styles.screen}>
      <ProfileHeader title="Profile & Settings" onPressBack={() => router.back()} />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        <ProfileSummaryCard
          avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
          name={currentMember.name}
          phoneNumber={currentMemberPhone}
          onPress={() => {
            // Profile Details drill-down — not yet built, no-op for now.
          }}
        />

        {wearableConnection ? (
          <WearableConnectedCard
            providerLabel={providerLabels[wearableConnection.provider]}
            syncLabel={
              wearableConnection.lastSyncedAt ? 'Last synced: Just now' : 'Not yet synced'
            }
            onPressDisconnect={() => {
              // Disconnect wearable — stub, backend not wired yet.
            }}
          />
        ) : (
          <WearableConnectCard
            onPressConnect={() => {
              // Connect Wearable flow — onboarding screen exists at (auth)/connect-wearable.
            }}
          />
        )}

        <SettingsListCard>
          {[
            ...(isPrimary
              ? [
                  <SettingsListItem
                    key="members"
                    icon={IconListCheck}
                    label="Members & Permissions"
                    onPress={() => {}}
                  />,
                ]
              : []),
            <SettingsListItem
              key="subscription"
              icon={IconCash}
              label="Manage Subscription"
              onPress={() => router.push('/subscription/payment-gateway')}
            />,
            ...(isPrimary
              ? [
                  <SettingsListItem
                    key="hidden-members"
                    icon={IconUsers}
                    label="Hidden Members"
                    onPress={() => {}}
                  />,
                ]
              : []),
            <SettingsListItem
              key="documentation"
              icon={IconFileDescription}
              label="Documentation"
              onPress={() => {}}
            />,
            <SettingsListItem
              key="terms"
              icon={IconArticle}
              label="Terms & Conditions"
              onPress={() => {}}
            />,
            <SettingsListItem
              key="privacy"
              icon={IconFileTextShield}
              label="Privacy Policy"
              onPress={() => {}}
            />,
            <SettingsListItem
              key="logout"
              icon={IconLogout}
              label="Log Out"
              destructive
              onPress={() => router.replace('/(auth)/login')}
            />,
          ]}
        </SettingsListCard>
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
    gap: 16,
  },
});
