import { IconUserPlus } from '@tabler/icons-react-native';
import { useRouter } from 'expo-router';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { HomeHeader } from '@/features/home/components/HomeHeader';
import { MemberSyncCard } from '@/features/home/components/MemberSyncCard';
import { useMembers } from '@/features/members/useMembers';
import { Colors, Fonts, Radii } from '@/theme/tokens';

/**
 * "Manage Members" (Figma node 197:9935) — distinct from Home's "Shared with you" list:
 * shows every member in the account (not filtered by pinned/hidden) plus an Add Member CTA.
 * Per-member "X metrics" / "Last synced" captions in the Figma reference are skipped here —
 * real wearable sync isn't built yet (TRD M1/M2), so "No wearable connected" is the honest
 * state for every member today, same caption the Figma design itself uses for that state.
 */
export function MemberListScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { self, others, isLoading, errorMessage } = useMembers();

  return (
    <View style={styles.screen}>
      <HomeHeader
        greetingName={self?.name.split(' ')[0] ?? ''}
        statusMessage="Manage your family members"
        unreadNotificationCount={0}
        onPressNotifications={() => router.push('/notifications')}
        onPressProfile={() => router.push('/profile')}
      />

      <ScrollView
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}
        showsVerticalScrollIndicator={false}>
        {isLoading && !self ? (
          <ActivityIndicator style={styles.loading} />
        ) : errorMessage ? (
          <Text style={styles.errorText}>{errorMessage}</Text>
        ) : (
          <>
            <Text style={styles.sectionTitle}>Members</Text>

            {self && (
              <MemberSyncCard
                avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
                displayName={`${self.name} (You)`}
                syncLabel="No wearable connected"
                onPress={() => router.push(`/member/${self.id}`)}
              />
            )}

            {others.map((member) => (
              <MemberSyncCard
                key={member.id}
                avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
                displayName={member.nickname ?? member.name}
                syncLabel="No wearable connected"
                onPress={() => router.push(`/member/${member.id}`)}
              />
            ))}

            <Pressable
              style={styles.addButton}
              onPress={() => router.push('/members/invite')}
              accessibilityRole="button"
              accessibilityLabel="Add Member">
              <IconUserPlus size={24} color={Colors.accent} strokeWidth={1.75} />
              <Text style={styles.addLabel}>Add Member</Text>
            </Pressable>
          </>
        )}
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
    gap: 12,
  },
  sectionTitle: {
    fontFamily: Fonts.family,
    fontWeight: '500',
    fontSize: 20,
    lineHeight: 28,
    color: Colors.textPrimary,
    marginBottom: 4,
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
  addButton: {
    height: 56,
    borderRadius: Radii.row,
    // Figma node 197:9935: filled lavender tint, not a white/bordered button.
    backgroundColor: Colors.tabBarTrack,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  addLabel: {
    fontFamily: Fonts.family,
    fontWeight: '600',
    fontSize: 16,
    lineHeight: 24,
    color: Colors.accent,
  },
});
