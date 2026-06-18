import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { EmptySharedState } from '@/features/home/components/EmptySharedState';
import { HomeHeader } from '@/features/home/components/HomeHeader';
import { MemberSyncCard } from '@/features/home/components/MemberSyncCard';
import { Colors, Fonts } from '@/theme/tokens';
import type { Member } from '@/types';

const currentMember: Member = {
  id: 'member-self',
  accountId: 'account-self',
  role: 'primary',
  name: 'Prasanna Kumar',
  pinned: true,
  hidden: false,
};

const sharedMembers: Member[] = [];

export function HomeScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();

  return (
    <View style={styles.screen}>
      <HomeHeader
        greetingName={currentMember.name.split(' ')[0]}
        statusMessage="Everything looks stable"
        unreadNotificationCount={3}
        onPressNotifications={() => router.push('/notifications')}
        onPressProfile={() => router.push('/profile')}
      />

      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + 24 },
        ]}
        showsVerticalScrollIndicator={false}>
        <MemberSyncCard
          avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
          displayName={`${currentMember.name} (You)`}
          syncLabel="Last synced: Just now"
          onPress={() => router.push(`/member/${currentMember.id}`)}
        />

        <Text style={styles.sectionTitle}>Shared with you</Text>

        {sharedMembers.length === 0 ? (
          <EmptySharedState />
        ) : (
          sharedMembers.map((member) => (
            <MemberSyncCard
              key={member.id}
              avatarSource={require('../../../assets/images/avatars/prasanna-kumar.png')}
              displayName={member.nickname ?? member.name}
              syncLabel="Last synced: Just now"
              onPress={() => router.push(`/member/${member.id}`)}
            />
          ))
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
});
