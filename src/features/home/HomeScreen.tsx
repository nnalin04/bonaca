import { useRouter } from 'expo-router';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { EmptySharedState } from '@/features/home/components/EmptySharedState';
import { HomeHeader } from '@/features/home/components/HomeHeader';
import { MemberSyncCard } from '@/features/home/components/MemberSyncCard';
import { SharedMemberCard } from '@/features/home/components/SharedMemberCard';
import { useMembers } from '@/features/members';
import { useNotifications } from '@/features/notifications';
import { Colors, Fonts } from '@/theme/tokens';

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
