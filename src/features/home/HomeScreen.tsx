import {
  IconRefresh,
  IconTrendingDown,
  IconTrendingUp,
  IconWaveSine,
} from '@tabler/icons-react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { EmptySharedState } from '@/features/home/components/EmptySharedState';
import { HomeHeader } from '@/features/home/components/HomeHeader';
import { MemberSyncCard } from '@/features/home/components/MemberSyncCard';
import { useMembers } from '@/features/members';
import { useNotifications } from '@/features/notifications';
import { Colors, Fonts, Radii } from '@/theme/tokens';
import type { Member } from '@/types';

const alertChips = [
  {
    label: '2 High',
    icon: IconTrendingUp,
    color: '#e97961',
    backgroundColor: 'rgba(233, 121, 97, 0.1)',
  },
  {
    label: '2 Low',
    icon: IconTrendingDown,
    color: '#5b8def',
    backgroundColor: 'rgba(91, 141, 239, 0.1)',
  },
  {
    label: '2 Normal',
    icon: IconWaveSine,
    color: '#8b6f9c',
    backgroundColor: 'rgba(139, 111, 156, 0.1)',
  },
];

export function HomeScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { self, others, isLoading, errorMessage } = useMembers();
  const { notifications } = useNotifications(self?.id);
  const unreadNotificationCount = notifications.filter((n) => !n.read).length;

  return (
    <View style={styles.screen}>
      <HomeHeader
        greetingName={self?.name.split(' ')[0] ?? ''}
        statusMessage={self?.statusMessage ?? 'Everything looks stable'}
        unreadNotificationCount={unreadNotificationCount}
        onPressNotifications={() => router.push('/notifications')}
        onPressProfile={() => router.push('/profile')}
      />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        {isLoading && !self ? (
          <ActivityIndicator style={styles.loading} />
        ) : errorMessage ? (
          <Text style={styles.errorText}>{errorMessage}</Text>
        ) : (
          <>
            {self && (
              <MemberSyncCard
                avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
                displayName={`${self.name} (You)`}
                syncLabel="Last synced: Just now"
                onPress={() => router.push(`/member/${self.id}`)}
              />
            )}

            <Text style={styles.sectionTitle}>Shared with you</Text>

            {others.length === 0 ? (
              <EmptySharedState />
            ) : (
              <View style={styles.sharedRows}>
                {others.map((member, index) => (
                  <SharedMemberCard
                    key={member.id}
                    member={member}
                    syncLabel={`Last synced: ${index === 0 ? '1 hr ago' : 'Just now'}`}
                    avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
                    onPress={() => router.push(`/member/${member.id}`)}
                  />
                ))}
              </View>
            )}
          </>
        )}
      </ScrollView>
    </View>
  );
}

function SharedMemberCard({
  member,
  syncLabel,
  avatarSource,
  onPress,
}: {
  member: Member;
  syncLabel: string;
  avatarSource: number;
  onPress: () => void;
}) {
  return (
    <Pressable
      style={styles.sharedCard}
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={member.nickname ?? member.name}
    >
      <Image
        source={avatarSource}
        style={styles.sharedAvatar}
        contentFit="cover"
      />
      <View style={styles.sharedContent}>
        <View style={styles.sharedTopRow}>
          <View style={styles.sharedTextBlock}>
            <Text style={styles.sharedName}>
              {member.nickname ?? member.name}
            </Text>
            <Text style={styles.sharedSync}>{syncLabel}</Text>
          </View>
          <IconRefresh size={24} color={Colors.accent} strokeWidth={1.75} />
        </View>

        <View style={styles.alertRow}>
          {alertChips.map((chip) => {
            const ChipIcon = chip.icon;
            return (
              <View
                key={chip.label}
                style={[
                  styles.alertChip,
                  { backgroundColor: chip.backgroundColor },
                ]}
              >
                <ChipIcon size={14} color={chip.color} strokeWidth={1.75} />
                <Text style={styles.alertChipText}>{chip.label}</Text>
              </View>
            );
          })}
        </View>
      </View>
    </Pressable>
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
  sectionTitle: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 20,
    lineHeight: 28,
    color: Colors.textPrimary,
    marginTop: 24,
    marginBottom: 12,
  },
  sharedRows: {
    gap: 16,
  },
  sharedCard: {
    width: '100%',
    height: 102,
    borderRadius: Radii.card,
    borderWidth: 1,
    borderColor: Colors.cardBorder,
    backgroundColor: Colors.white,
    flexDirection: 'row',
    alignItems: 'flex-start',
    padding: 12,
    gap: 8,
  },
  sharedAvatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
  },
  sharedContent: {
    flex: 1,
  },
  sharedTopRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
  },
  sharedTextBlock: {
    gap: 2,
  },
  sharedName: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 16,
    lineHeight: 22,
    color: Colors.textPrimary,
  },
  sharedSync: {
    fontFamily: Fonts.family,
    fontWeight: '400',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
  alertRow: {
    marginTop: 14,
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 8,
  },
  alertChip: {
    height: 24,
    minWidth: 72,
    borderRadius: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingHorizontal: 8,
  },
  alertChipText: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 12,
    lineHeight: 16,
    color: Colors.textSecondary,
  },
  loading: {
    marginTop: 48,
  },
  errorText: {
    marginTop: 48,
    textAlign: 'center',
    color: Colors.error,
    fontFamily: Fonts.family,
  },
});
